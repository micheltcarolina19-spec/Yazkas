package usta.veterinaria.infrastructure.adapter.in.web.admin;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import usta.veterinaria.application.port.in.admin.GestionarBitacoraServicioUseCase;
import usta.veterinaria.application.port.in.usuario.GestionarCitasUseCase;
import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.admin.BitacoraServicio;
import usta.veterinaria.domain.model.admin.TipoBano;
import usta.veterinaria.domain.model.mascota.Desparasitacion;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.domain.model.mascota.TipoDesparasitacion;
import usta.veterinaria.domain.model.mascota.Vacunacion;
import usta.veterinaria.domain.model.usuario.AgendamientoCita;
import usta.veterinaria.domain.model.usuario.EstadoCita;
import usta.veterinaria.domain.model.usuario.MascotaCita;
import usta.veterinaria.domain.model.usuario.Usuario;
import usta.veterinaria.domain.service.MonedaColombiana;
import usta.veterinaria.domain.service.RelojColombia;
import usta.veterinaria.domain.service.admin.BitacoraServicioService;
import usta.veterinaria.domain.service.usuario.CitaService;
import usta.veterinaria.infrastructure.adapter.in.web.admin.CalendarioVista.AccionVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.CalendarioVista.BitacoraVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.CalendarioVista.BloqueVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.CalendarioVista.CeldaVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.CalendarioVista.ChipVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.CalendarioVista.DesparasitacionVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.CalendarioVista.DiaVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.CalendarioVista.MascotaVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.CalendarioVista.OpcionVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.CalendarioVista.ResumenVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.CalendarioVista.VacunaVista;
import usta.veterinaria.infrastructure.adapter.in.web.sesion.SoloAdmin;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// Adaptador de entrada WEB: calendario de citas del administrador (renderizado en el servidor).
//   - GET  /admin/citas?vista=dia|semana|mes&fecha=2026-11-12   -> calendario
//   - GET  /admin/citas?...&reprogramar=ID | &cancelar=ID        -> calendario con el diálogo abierto
//   - POST /admin/citas/reprogramar | /cancelar                  -> ejecuta la acción y redirige (303)
//   - GET  /admin/citas?...&bitacora=AG_ID&mascota=MC_ID        -> calendario con el modal de bitácora abierto
//   - POST /admin/citas/bitacora                                 -> guarda la bitácora de UNA mascota; si todas las
//                                                                   mascotas de la cita tienen precio, se completa sola
// Usa el MISMO caso de uso que el adaptador REST (GestionarCitasUseCase).
@Path("/admin/citas")
@ApplicationScoped
@SoloAdmin
public class AdminCitasResource {

    private static final Logger LOG = Logger.getLogger(AdminCitasResource.class);

    private static final int HORA_INICIO = 7;     // el calendario crece si hay citas antes de esta hora
    private static final int HORA_FIN = 20;       // ...o después de esta
    private static final int ALTO_HORA = 72;      // px por hora

    private static final String[] DIAS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
    private static final String[] DIAS_LARGO = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
    private static final String[] MESES = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio",
            "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
    private static final String[] MESES_CORTO = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};

    @Inject
    @Location("admin/adminCitas.html")
    Template index;

    // Lo que se encontró en la mascota (se guarda dentro de "observaciones_peluqueria")
    private static final List<String> HALLAZGOS = List.of(
            "Sin novedades", "Pelo en exceso", "Suciedad", "Heridas o irritaciones", "Parásitos");
    private static final String OTRA = "Otra";
    private static final String PRE_ENCONTRO = "En la mascota se encontró: ";
    private static final String PRE_OBSERVACIONES = "Observaciones: ";
    private static final String PRE_RECOMENDACIONES = "Recomendaciones: ";
    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");

    @Inject
    GestionarCitasUseCase citas;

    @Inject
    GestionarBitacoraServicioUseCase bitacoras;

    // El dominio decide cuántos minutos bloquea cada duración (BAJA, MEDIA, ALTA)
    private final CitaService citaService = new CitaService();

    // Reglas de la bitácora: servicios válidos, mascotas registradas y total
    private final BitacoraServicioService bitacoraService = new BitacoraServicioService();

    // ===================== Páginas =====================

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance mostrar(@QueryParam("vista") String vista,
                                    @QueryParam("fecha") String fecha,
                                    @QueryParam("reprogramar") Long reprogramar,
                                    @QueryParam("cancelar") Long cancelar,
                                    @QueryParam("bitacora") Long bitacora,
                                    @QueryParam("mascota") Long mascota,
                                    @QueryParam("ok") String ok) {
        return pagina(vistaValida(vista), fechaOHoy(fecha), ok, null, reprogramar, cancelar, bitacora, mascota, null);
    }

    // ===================== Acciones =====================

    @POST
    @Path("/reprogramar")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response reprogramar(@FormParam("agendamientoId") Long agendamientoId,
                                @FormParam("nuevaFecha") String nuevaFecha,
                                @FormParam("nuevaHora") String nuevaHora,
                                @FormParam("vista") String vista,
                                @FormParam("fecha") String fecha) {
        String v = vistaValida(vista);
        LocalDate actual = fechaOHoy(fecha);
        try {
            if (nuevaFecha == null || nuevaFecha.isBlank() || nuevaHora == null || nuevaHora.isBlank()) {
                throw new BusinessRuleException("Debes indicar la nueva fecha y hora.");
            }
            LocalDate f = LocalDate.parse(nuevaFecha);
            citas.reprogramar(agendamientoId, f, LocalTime.parse(nuevaHora));
            // Se lleva el calendario a la nueva fecha para que el administrador vea la cita movida
            return redirigir(v, f, "Cita reprogramada correctamente.");
        } catch (DateTimeParseException e) {
            return conError(v, actual, agendamientoId, null, "La fecha u hora indicada no es válida.");
        } catch (BusinessRuleException e) {
            return conError(v, actual, agendamientoId, null, e.getMessage());
        } catch (RuntimeException e) {
            LOG.error("No se pudo reprogramar la cita " + agendamientoId, e);
            return conError(v, actual, agendamientoId, null, "No se pudo reprogramar: " + e.getMessage());
        }
    }

    @POST
    @Path("/cancelar")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response cancelar(@FormParam("agendamientoId") Long agendamientoId,
                             @FormParam("vista") String vista,
                             @FormParam("fecha") String fecha) {
        String v = vistaValida(vista);
        LocalDate actual = fechaOHoy(fecha);
        try {
            citas.cancelar(agendamientoId);
            return redirigir(v, actual, "Cita cancelada. El horario quedó libre.");
        } catch (BusinessRuleException e) {
            return conError(v, actual, null, agendamientoId, e.getMessage());
        } catch (RuntimeException e) {
            LOG.error("No se pudo cancelar la cita " + agendamientoId, e);
            return conError(v, actual, null, agendamientoId, "No se pudo cancelar: " + e.getMessage());
        }
    }

    // Guarda la bitácora de UNA mascota. Si con esto todas las mascotas de la cita ya tienen precio,
    // la cita queda COMPLETADA y su total se calcula solo. Si falta otra mascota, se abre su bitácora.
    @POST
    @Path("/bitacora")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response guardarBitacora(@FormParam("agendamientoId") Long agendamientoId,
                                    @FormParam("mascotaCitaId") Long mascotaCitaId,
                                    @FormParam("servicios") List<String> servicios,
                                    @FormParam("tipoBano") String tipoBano,
                                    @FormParam("hallazgos") List<String> hallazgos,
                                    @FormParam("otroHallazgo") String otroHallazgo,
                                    @FormParam("observaciones") String observaciones,
                                    @FormParam("recomendaciones") String recomendaciones,
                                    @FormParam("fotoSalida") String fotoSalida,
                                    @FormParam("precio") String precio,
                                    // Vacunación (tabla vacunaciones)
                                    @FormParam("vacNombre") String vacNombre,
                                    @FormParam("vacFecha") String vacFecha,
                                    @FormParam("vacRefuerzo") String vacRefuerzo,
                                    @FormParam("vacLote") String vacLote,
                                    @FormParam("vacDosis") String vacDosis,
                                    @FormParam("vacObservaciones") String vacObservaciones,
                                    // Desparasitación (tabla desparasitaciones)
                                    @FormParam("despMarca") String despMarca,
                                    @FormParam("despTipo") String despTipo,
                                    @FormParam("despFecha") String despFecha,
                                    @FormParam("despLote") String despLote,
                                    @FormParam("despObservaciones") String despObservaciones,
                                    @FormParam("vista") String vista,
                                    @FormParam("fecha") String fecha) {
        String v = vistaValida(vista);
        LocalDate actual = fechaOHoy(fecha);

        // Lo escrito se conserva para volver a pintar el formulario si algo falla
        BitacoraServicio borrador = BitacoraServicio.builder()
                .tipoServicio(servicios == null ? "" : String.join(", ", servicios))
                .tipoBano(tipoBanoDe(tipoBano))
                .fotoSalida(fotoSalida)
                .precioSubtotal(leerPrecio(precio))
                .observacionesPeluqueria(componerObservaciones(hallazgos, otroHallazgo, observaciones, recomendaciones))
                .vacunacion(Vacunacion.builder()
                        .nombreVacuna(texto(vacNombre))
                        .fechaAplicacion(leerFecha(vacFecha))
                        .fechaRefuerzo(leerFecha(vacRefuerzo))
                        .numeroLote(texto(vacLote))
                        .numeroDosis(leerEntero(vacDosis))
                        .observaciones(texto(vacObservaciones))
                        .build())
                .desparasitacion(Desparasitacion.builder()
                        .marca(texto(despMarca))
                        .tipo(tipoDesparasitacionDe(despTipo))
                        .fechaAplicacion(leerFecha(despFecha))
                        .numeroLote(texto(despLote))
                        .observaciones(texto(despObservaciones))
                        .build())
                .build();
        try {
            List<String> marcados = serviciosMarcados(borrador.getTipoServicio());
            validarFechaFormulario(marcados.contains(BitacoraServicioService.VACUNACION), vacFecha, vacRefuerzo,
                    "de la vacuna");
            validarFechaFormulario(marcados.contains(BitacoraServicioService.DESPARASITACION), despFecha, null,
                    "de la desparasitación");
            if (borrador.getPrecioSubtotal() == null || borrador.getPrecioSubtotal().signum() <= 0) {
                throw new BusinessRuleException("Escribe el precio (subtotal) del servicio de la mascota.");
            }
            bitacoras.registrarParaMascota(agendamientoId, mascotaCitaId, borrador.getTipoServicio(),
                    borrador.getFotoSalida(), borrador.getTipoBano(), borrador.getPrecioSubtotal(),
                    borrador.getObservacionesPeluqueria(),
                    marcados.contains(BitacoraServicioService.VACUNACION) ? borrador.getVacunacion() : null,
                    marcados.contains(BitacoraServicioService.DESPARASITACION) ? borrador.getDesparasitacion() : null);

            AgendamientoCita agendamiento = citas.buscarPorId(agendamientoId);
            List<MascotaCita> delAgendamiento = citas.listarCitasDelAgendamiento(agendamientoId);
            String nombre = delAgendamiento.stream().filter(c -> Objects.equals(c.getId(), mascotaCitaId))
                    .findFirst().map(c -> nombreDe(c.getMascota())).orElse("la mascota");

            if (agendamiento.getEstado() == EstadoCita.COMPLETADA) {
                return redirigir(v, actual, "¡Cita completada! Bitácora de " + nombre + " guardada. Total de la cita: "
                        + MonedaColombiana.formatear(agendamiento.getPrecioTotal()));
            }

            // Falta otra mascota: se abre directamente su bitácora para llenarla una por una
            List<BitacoraServicio> registradas = bitacoras.listarPorAgendamiento(agendamientoId);
            MascotaCita pendiente = delAgendamiento.stream()
                    .filter(c -> bitacoraService.buscarDeMascota(registradas, c.getId()) == null)
                    .findFirst().orElse(null);
            String mensaje = "Bitácora de " + nombre + " guardada."
                    + (pendiente == null ? "" : " Falta la bitácora de " + nombreDe(pendiente.getMascota())
                    + " para completar la cita.");
            String extra = pendiente == null ? "" : "&bitacora=" + agendamientoId + "&mascota=" + pendiente.getId();
            return Response.seeOther(URI.create(url(v, actual, extra + "&ok="
                    + URLEncoder.encode(mensaje, StandardCharsets.UTF_8)))).build();
        } catch (BusinessRuleException e) {
            return conErrorBitacora(v, actual, agendamientoId, mascotaCitaId, borrador, e.getMessage());
        } catch (RuntimeException e) {
            LOG.error("No se pudo guardar la bitácora de la cita " + agendamientoId, e);
            return conErrorBitacora(v, actual, agendamientoId, mascotaCitaId, borrador,
                    "No se pudo guardar la bitácora: " + e.getMessage());
        }
    }

    private Response conErrorBitacora(String vista, LocalDate fecha, Long agendamientoId, Long mascotaCitaId,
                                      BitacoraServicio borrador, String error) {
        return Response.ok(pagina(vista, fecha, null, error, null, null, agendamientoId, mascotaCitaId, borrador)
                .render()).type(MediaType.TEXT_HTML).build();
    }

    private Response redirigir(String vista, LocalDate fecha, String mensaje) {
        String destino = url(vista, fecha, "&ok=" + URLEncoder.encode(mensaje, StandardCharsets.UTF_8));
        return Response.seeOther(URI.create(destino)).build();
    }

    private Response conError(String vista, LocalDate fecha, Long reprogramarId, Long cancelarId, String error) {
        return Response.ok(pagina(vista, fecha, null, error, reprogramarId, cancelarId, null, null, null).render())
                .type(MediaType.TEXT_HTML).build();
    }

    // ===================== Armado de la página =====================

    private TemplateInstance pagina(String vista, LocalDate fecha, String ok, String error,
                                    Long reprogramarId, Long cancelarId,
                                    Long bitacoraId, Long mascotaCitaId, BitacoraServicio borrador) {
        LocalDate hoy = LocalDate.now(RelojColombia.reloj());

        LocalDate desde;
        LocalDate hasta;
        switch (vista) {
            case "dia" -> { desde = fecha; hasta = fecha; }
            case "mes" -> { desde = lunesDe(fecha.withDayOfMonth(1)); hasta = desde.plusDays(41); }
            default -> { desde = lunesDe(fecha); hasta = desde.plusDays(6); }
        }

        List<Grupo> grupos = cargarGrupos(desde, hasta);

        TemplateInstance pagina = index.data("vista", vista)
                .data("fechaIso", fecha.toString())
                .data("periodo", periodo(vista, fecha))
                .data("hrefAnterior", url(vista, desplazar(vista, fecha, -1), ""))
                .data("hrefSiguiente", url(vista, desplazar(vista, fecha, 1), ""))
                .data("hrefHoy", url(vista, hoy, ""))
                .data("hrefDia", url("dia", fecha, ""))
                .data("hrefSemana", url("semana", fecha, ""))
                .data("hrefMes", url("mes", fecha, ""))
                .data("resumen", resumen(grupos))
                .data("ok", ok)
                .data("error", error)
                .data("reprogramar", accion(reprogramarId, vista, fecha))
                .data("cancelar", accion(cancelarId, vista, fecha))
                .data("bitacora", bitacoraVista(bitacoraId, mascotaCitaId, vista, fecha, borrador));

        if ("mes".equals(vista)) {
            return pagina.data("celdas", celdasDelMes(desde, fecha, hoy, grupos))
                    .data("dias", List.of())
                    .data("horas", List.of())
                    .data("altoHora", ALTO_HORA);
        }

        // Rango de horas visible: el normal, ampliado si alguna cita se sale de él
        int hIni = HORA_INICIO;
        int hFin = HORA_FIN;
        for (Grupo g : grupos) {
            hIni = Math.min(hIni, g.ini / 60);
            hFin = Math.max(hFin, Math.min(24, (g.fin + 59) / 60));
        }

        int nDias = "dia".equals(vista) ? 1 : 7;
        List<DiaVista> dias = new ArrayList<>();
        for (int i = 0; i < nDias; i++) {
            dias.add(diaVista(desde.plusDays(i), i, nDias, grupos, hIni, hFin, hoy, vista, fecha));
        }

        List<String> horas = new ArrayList<>();
        for (int h = hIni; h < hFin; h++) {
            horas.add((h % 12 == 0 ? 12 : h % 12) + ":00 " + (h >= 12 ? "PM" : "AM"));
        }
        return pagina.data("dias", dias).data("horas", horas).data("altoHora", ALTO_HORA).data("celdas", List.of());
    }

    // ===================== Citas -> grupos (un agendamiento = un bloque) =====================

    // Las mascotas de un mismo agendamiento, a la misma fecha y hora, se muestran juntas en un solo bloque
    private static final class Grupo {
        final AgendamientoCita agendamiento;
        final LocalDate fecha;
        final LocalTime hora;
        final List<MascotaCita> citas = new ArrayList<>();
        int ini;
        int fin;
        int carril;
        int carriles = 1;
        // Bitácoras registradas para este agendamiento
        List<BitacoraServicio> bitacoras = new ArrayList<>();

        Grupo(AgendamientoCita agendamiento, LocalDate fecha, LocalTime hora) {
            this.agendamiento = agendamiento;
            this.fecha = fecha;
            this.hora = hora;
        }

        EstadoCita estado() {
            return agendamiento == null || agendamiento.getEstado() == null ? EstadoCita.PROGRAMADA : agendamiento.getEstado();
        }
    }

    private List<Grupo> cargarGrupos(LocalDate desde, LocalDate hasta) {
        Map<String, Grupo> grupos = new LinkedHashMap<>();
        for (MascotaCita c : citas.listarCitasPorRango(desde, hasta)) {
            if (c == null || c.getMascota() == null || c.getFechaAgendamiento() == null
                    || c.getHoraAgendamiento() == null || c.getDuracionCita() == null) {
                continue;
            }
            AgendamientoCita ag = c.getAgendamientoCita();
            if (ag != null && ag.getEstado() == EstadoCita.CANCELADA) {
                continue;   // las canceladas no ocupan horario
            }
            LocalDate f = c.getFechaAgendamiento();
            if (f.isBefore(desde) || f.isAfter(hasta)) {
                continue;
            }
            String clave = (ag == null ? "-" : String.valueOf(ag.getId())) + "|" + f + "|" + c.getHoraAgendamiento();
            grupos.computeIfAbsent(clave, k -> new Grupo(ag, f, c.getHoraAgendamiento())).citas.add(c);
        }
        cargarBitacoras(grupos.values());
        for (Grupo g : grupos.values()) {
            g.ini = g.hora.getHour() * 60 + g.hora.getMinute();
            int mayor = 0;
            for (MascotaCita c : g.citas) {
                mayor = Math.max(mayor, (int) citaService.minutosReservados(c.getDuracionCita()));
            }
            g.fin = g.ini + mayor;
        }
        return new ArrayList<>(grupos.values());
    }

    // Trae en una sola consulta las bitácoras de todas las citas visibles y las reparte por agendamiento
    private void cargarBitacoras(Collection<Grupo> grupos) {
        Map<Long, List<Grupo>> porAgendamiento = new HashMap<>();
        for (Grupo g : grupos) {
            if (g.agendamiento != null && g.agendamiento.getId() != null) {
                porAgendamiento.computeIfAbsent(g.agendamiento.getId(), k -> new ArrayList<>()).add(g);
            }
        }
        if (porAgendamiento.isEmpty()) {
            return;
        }
        try {
            for (BitacoraServicio b : bitacoras.listarPorAgendamientos(porAgendamiento.keySet())) {
                if (b.getAgendamientoCita() == null) {
                    continue;
                }
                for (Grupo g : porAgendamiento.getOrDefault(b.getAgendamientoCita().getId(), List.of())) {
                    g.bitacoras.add(b);
                }
            }
        } catch (RuntimeException e) {
            LOG.warn("No se pudieron cargar las bitácoras del calendario: " + e.getMessage());
        }
    }

    // Reparte los bloques que se cruzan en carriles (columnas) para mostrarlos lado a lado
    private void asignarCarriles(List<Grupo> lista) {
        lista.sort(Comparator.comparingInt((Grupo g) -> g.ini).thenComparingInt(g -> g.fin));
        List<Grupo> actual = new ArrayList<>();
        int finActual = -1;
        for (Grupo g : lista) {
            if (!actual.isEmpty() && g.ini >= finActual) {
                cerrar(actual);
                actual = new ArrayList<>();
                finActual = -1;
            }
            Set<Integer> usados = new HashSet<>();
            for (Grupo otro : actual) {
                if (otro.fin > g.ini) {
                    usados.add(otro.carril);
                }
            }
            int carril = 0;
            while (usados.contains(carril)) {
                carril++;
            }
            g.carril = carril;
            actual.add(g);
            finActual = Math.max(finActual, g.fin);
        }
        if (!actual.isEmpty()) {
            cerrar(actual);
        }
    }

    private void cerrar(List<Grupo> grupo) {
        int n = grupo.stream().mapToInt(g -> g.carril).max().orElse(0) + 1;
        grupo.forEach(g -> g.carriles = n);
    }

    // ===================== Vistas: semana / día =====================

    private DiaVista diaVista(LocalDate d, int colIdx, int nCols, List<Grupo> todos, int hIni, int hFin,
                              LocalDate hoy, String vista, LocalDate fechaVista) {
        List<Grupo> delDia = new ArrayList<>(todos.stream().filter(g -> g.fecha.equals(d)).toList());
        asignarCarriles(delDia);

        int altoTotal = (hFin - hIni) * ALTO_HORA;
        List<BloqueVista> bloques = new ArrayList<>();
        for (Grupo g : delDia) {
            bloques.add(bloque(g, colIdx, nCols, hIni, altoTotal, vista, fechaVista));
        }

        int ahoraTop = -1;
        if (d.equals(hoy)) {
            LocalTime ahora = LocalTime.now(RelojColombia.reloj());
            int min = ahora.getHour() * 60 + ahora.getMinute();
            if (min >= hIni * 60 && min <= hFin * 60) {
                ahoraTop = (min - hIni * 60) * ALTO_HORA / 60;
            }
        }
        return new DiaVista(DIAS[d.getDayOfWeek().getValue() - 1], d.getDayOfMonth() + " " + MESES_CORTO[d.getMonthValue() - 1],
                d.equals(hoy), ahoraTop, bloques, url("dia", d, ""));
    }

    private BloqueVista bloque(Grupo g, int colIdx, int nCols, int hIni, int altoTotal, String vista, LocalDate fechaVista) {
        LocalDate hoy = LocalDate.now(RelojColombia.reloj());
        int top = (g.ini - hIni * 60) * ALTO_HORA / 60;
        int alto = Math.max((g.fin - g.ini) * ALTO_HORA / 60 - 4, 40);
        double izquierda = redondear(g.carril * 100.0 / g.carriles);
        double ancho = redondear(100.0 / g.carriles);

        // La tarjeta del hover se abre hacia el lado que tenga espacio
        double posicion = (colIdx + (double) g.carril / g.carriles) / nCols;
        String lado = posicion >= 0.5 ? "izq" : "der";
        String vertical = top < altoTotal * 0.5 ? "abajo" : "arriba";

        EstadoCita estado = g.estado();
        String categoria = categoriaDe(g);
        Long id = g.agendamiento == null ? null : g.agendamiento.getId();
        List<MascotaVista> mascotas = g.citas.stream()
                .map(c -> vistaMascota(c, hoy, id, g.bitacoras, vista, fechaVista)).toList();
        boolean modificable = id != null && (estado == EstadoCita.PROGRAMADA || estado == EstadoCita.REPROGRAMADA);
        int registradas = (int) mascotas.stream().filter(MascotaVista::registrada).count();

        return new BloqueVista(categoria, servicioDe(g, categoria), estadoClase(estado), estadoTexto(estado),
                unir(mascotas.stream().map(MascotaVista::nombre).toList()),
                rango(g.ini, g.fin), fechaLarga(g.fecha),
                top, alto, izquierda, ancho, lado, vertical, modificable, mascotas.size() > 1,
                mascotas,
                modificable ? url(vista, fechaVista, "&reprogramar=" + id) : "",
                modificable ? url(vista, fechaVista, "&cancelar=" + id) : "",
                totalTexto(g), registradas + "/" + mascotas.size(),
                estado == EstadoCita.COMPLETADA, id != null);
    }

    private MascotaVista vistaMascota(MascotaCita cita, LocalDate hoy, Long agendamientoId,
                                      List<BitacoraServicio> delAgendamiento, String vista, LocalDate fechaVista) {
        Mascota m = cita.getMascota();
        Usuario u = m.getUsuario();
        String nombre = m.getNombre() == null || m.getNombre().isBlank() ? "Mascota" : m.getNombre();
        String duenio = u == null ? "" : ((u.getNombre() == null ? "" : u.getNombre()) + " "
                + (u.getApellido() == null ? "" : u.getApellido())).trim();
        long horas = citaService.minutosReservados(cita.getDuracionCita()) / 60;
        BitacoraServicio registro = bitacoraService.buscarDeMascota(delAgendamiento, cita.getId());
        return new MascotaVista(
                cita.getId(),
                nombre,
                m.getRaza() == null ? "" : m.getRaza(),
                edad(m.getFechaNacimiento(), hoy),
                m.getFotoMascota() == null ? "" : m.getFotoMascota().trim(),
                nombre.substring(0, 1).toUpperCase(),
                duenio,
                u == null || u.getCelular() == null ? "" : u.getCelular(),
                "Reserva de " + horas + " h",
                registro != null,
                registro == null ? "" : pesos(registro.getPrecioSubtotal()),
                agendamientoId == null ? ""
                        : url(vista, fechaVista, "&bitacora=" + agendamientoId + "&mascota=" + cita.getId()));
    }

    // ===================== Vista: mes =====================

    private List<CeldaVista> celdasDelMes(LocalDate desde, LocalDate fecha, LocalDate hoy, List<Grupo> grupos) {
        List<CeldaVista> celdas = new ArrayList<>();
        for (int i = 0; i < 42; i++) {
            LocalDate d = desde.plusDays(i);
            List<Grupo> delDia = grupos.stream().filter(g -> g.fecha.equals(d))
                    .sorted(Comparator.comparingInt((Grupo g) -> g.ini)).toList();
            List<ChipVista> chips = new ArrayList<>();
            for (Grupo g : delDia.stream().limit(3).toList()) {
                String categoria = categoriaDe(g);
                chips.add(new ChipVista(hora(g.ini),
                        unir(g.citas.stream().map(c -> c.getMascota().getNombre() == null ? "Mascota" : c.getMascota().getNombre()).toList()),
                        categoria, estadoClase(g.estado()), url("dia", d, ""), totalTexto(g)));
            }
            celdas.add(new CeldaVista(d.getDayOfMonth(), d.getMonthValue() != fecha.getMonthValue(), d.equals(hoy),
                    chips, Math.max(0, delDia.size() - 3), url("dia", d, "")));
        }
        return celdas;
    }

    // ===================== Resumen y diálogos =====================

    private ResumenVista resumen(List<Grupo> grupos) {
        int programadas = 0;
        int reprogramadas = 0;
        int completadas = 0;
        for (Grupo g : grupos) {
            switch (g.estado()) {
                case REPROGRAMADA -> reprogramadas++;
                case COMPLETADA -> completadas++;
                default -> programadas++;
            }
        }
        return new ResumenVista(grupos.size(), programadas, reprogramadas, completadas);
    }

    // Datos del agendamiento para el diálogo de reprogramar / cancelar (null si no existe)
    private AccionVista accion(Long agendamientoId, String vista, LocalDate fecha) {
        if (agendamientoId == null) {
            return null;
        }
        try {
            List<MascotaCita> delAgendamiento = citas.listarCitasDelAgendamiento(agendamientoId);
            if (delAgendamiento.isEmpty()) {
                return null;
            }
            MascotaCita primera = delAgendamiento.get(0);
            LocalTime hora = primera.getHoraAgendamiento().withSecond(0).withNano(0);
            String nombres = unir(delAgendamiento.stream()
                    .map(c -> c.getMascota().getNombre() == null ? "Mascota" : c.getMascota().getNombre()).toList());
            String resumen = nombres + " — " + fechaLarga(primera.getFechaAgendamiento())
                    + ", " + hora(hora.getHour() * 60 + hora.getMinute());
            return new AccionVista(agendamientoId, resumen, primera.getFechaAgendamiento().toString(),
                    hora.toString(), url(vista, fecha, ""));
        } catch (RuntimeException e) {
            LOG.warn("No se pudo cargar el agendamiento " + agendamientoId + ": " + e.getMessage());
            return null;
        }
    }

    // ===================== Servicio y estado =====================

    // El color del bloque sale de los servicios registrados en la bitácora ("bano" | "pelu" | "otros").
    // Mientras no haya bitácora, la cita se muestra como peluquería.
    private String categoriaDe(Grupo g) {
        Set<String> servicios = serviciosDelGrupo(g);
        if (servicios.contains(BitacoraServicioService.PELUQUERIA)) {
            return "pelu";
        }
        if (servicios.contains(BitacoraServicioService.BANO)) {
            return "bano";
        }
        if (servicios.contains(BitacoraServicioService.VACUNACION)
                || servicios.contains(BitacoraServicioService.DESPARASITACION)) {
            return "otros";
        }
        return "pelu";
    }

    // Texto del servicio: lo registrado ("Baño + Peluquería") o la categoría por defecto
    private String servicioDe(Grupo g, String categoria) {
        Set<String> servicios = serviciosDelGrupo(g);
        return servicios.isEmpty() ? servicioTexto(categoria) : String.join(" + ", servicios);
    }

    private Set<String> serviciosDelGrupo(Grupo g) {
        Set<String> servicios = new LinkedHashSet<>();
        for (BitacoraServicio b : g.bitacoras) {
            servicios.addAll(serviciosMarcados(b.getTipoServicio()));
        }
        return servicios;
    }

    // Total del calendario: el precio total si la cita está completada; si no, la suma parcial registrada
    private String totalTexto(Grupo g) {
        if (g.estado() == EstadoCita.COMPLETADA && g.agendamiento.getPrecioTotal() != null
                && g.agendamiento.getPrecioTotal().signum() > 0) {
            return pesos(g.agendamiento.getPrecioTotal());
        }
        BigDecimal suma = bitacoraService.total(g.bitacoras);
        return suma.signum() > 0 ? pesos(suma) : "";
    }

    private static String servicioTexto(String categoria) {
        return switch (categoria) {
            case "bano" -> "Baño";
            case "otros" -> "Otros servicios";
            default -> "Peluquería";
        };
    }

    private static String estadoClase(EstadoCita estado) {
        return switch (estado) {
            case REPROGRAMADA -> "reprogramada";
            case COMPLETADA -> "completada";
            default -> "programada";
        };
    }

    private static String estadoTexto(EstadoCita estado) {
        return switch (estado) {
            case REPROGRAMADA -> "Reprogramada";
            case COMPLETADA -> "Completada";
            default -> "Programada";
        };
    }

    // ===================== Bitácora (modal por mascota) =====================

    // Arma el modal "Bitácora de finalización" de una mascota de la cita (null si no se debe mostrar).
    // borrador = lo que el administrador escribió cuando hubo un error (para no perderlo).
    private BitacoraVista bitacoraVista(Long agendamientoId, Long mascotaCitaId, String vista, LocalDate fecha,
                                        BitacoraServicio borrador) {
        if (agendamientoId == null) {
            return null;
        }
        try {
            AgendamientoCita agendamiento = citas.buscarPorId(agendamientoId);
            if (agendamiento.getEstado() == EstadoCita.CANCELADA) {
                return null;
            }
            List<MascotaCita> delAgendamiento = citas.listarCitasDelAgendamiento(agendamientoId);
            if (delAgendamiento.isEmpty()) {
                return null;
            }
            List<BitacoraServicio> registradas = bitacoras.listarPorAgendamiento(agendamientoId);

            // Mascota pedida; si no viene, la primera que aún no tiene bitácora
            MascotaCita actual = delAgendamiento.stream().filter(c -> Objects.equals(c.getId(), mascotaCitaId))
                    .findFirst()
                    .orElseGet(() -> delAgendamiento.stream()
                            .filter(c -> bitacoraService.buscarDeMascota(registradas, c.getId()) == null)
                            .findFirst().orElse(delAgendamiento.get(0)));

            BitacoraServicio guardada = registradas.stream()
                    .filter(b -> b.getMascotaCita() != null && Objects.equals(b.getMascotaCita().getId(), actual.getId()))
                    .findFirst().orElse(null);
            BitacoraServicio datos = borrador != null ? borrador : guardada;

            // Lo ya cobrado por las OTRAS mascotas (para mostrar el total en vivo mientras se escribe)
            BigDecimal otras = BigDecimal.ZERO;
            for (BitacoraServicio b : registradas) {
                if (b != guardada && b.getPrecioSubtotal() != null) {
                    otras = otras.add(b.getPrecioSubtotal());
                }
            }
            boolean completada = agendamiento.getEstado() == EstadoCita.COMPLETADA;
            BigDecimal total = completada && agendamiento.getPrecioTotal() != null
                    ? agendamiento.getPrecioTotal() : bitacoraService.total(registradas);

            LocalDate hoy = LocalDate.now(RelojColombia.reloj());
            List<MascotaVista> pestanas = delAgendamiento.stream()
                    .map(c -> vistaMascota(c, hoy, agendamientoId, registradas, vista, fecha)).toList();
            int listas = (int) pestanas.stream().filter(MascotaVista::registrada).count();

            Mascota m = actual.getMascota();
            Usuario u = m.getUsuario();
            String nombre = nombreDe(m);
            LocalTime hora = actual.getHoraAgendamiento();
            String[] obs = separarObservaciones(datos == null ? null : datos.getObservacionesPeluqueria());
            Set<String> hallados = hallazgosMarcados(obs[0]);
            List<String> servicios = datos == null ? List.of() : serviciosMarcados(datos.getTipoServicio());
            TipoBano bano = datos == null || datos.getTipoBano() == null ? TipoBano.NORMAL : datos.getTipoBano();

            List<OpcionVista> opServicios = List.of(
                    new OpcionVista("bano", BitacoraServicioService.BANO, "Baño", servicios.contains(BitacoraServicioService.BANO)),
                    new OpcionVista("pelu", BitacoraServicioService.PELUQUERIA, "Peluquería", servicios.contains(BitacoraServicioService.PELUQUERIA)),
                    new OpcionVista("vacuna", BitacoraServicioService.VACUNACION, "Vacunación", servicios.contains(BitacoraServicioService.VACUNACION)),
                    new OpcionVista("desparasita", BitacoraServicioService.DESPARASITACION, "Desparasitación", servicios.contains(BitacoraServicioService.DESPARASITACION)));
            List<OpcionVista> opBano = List.of(
                    new OpcionVista("normal", TipoBano.NORMAL.name(), "Normal", bano == TipoBano.NORMAL),
                    new OpcionVista("medicado", TipoBano.MEDICADO.name(), "Medicado", bano == TipoBano.MEDICADO),
                    new OpcionVista("antipulgas", TipoBano.ANTIPULGAS.name(), "Antipulgas", bano == TipoBano.ANTIPULGAS));
            List<OpcionVista> opHallazgos = new ArrayList<>();
            for (int i = 0; i < HALLAZGOS.size(); i++) {
                String h = HALLAZGOS.get(i);
                opHallazgos.add(new OpcionVista("h" + i, h, h, hallados.contains(h)));
            }
            opHallazgos.add(new OpcionVista("otra", OTRA, OTRA, hallados.contains(OTRA)));

            // Vacuna y desparasitación: lo guardado (o lo escrito si hubo error). Fecha por defecto: la de la cita
            // (o hoy, si la cita es futura, porque la fecha de aplicación no puede ser futura)
            LocalDate fechaCita = actual.getFechaAgendamiento() == null ? hoy
                    : (actual.getFechaAgendamiento().isAfter(hoy) ? hoy : actual.getFechaAgendamiento());
            Vacunacion vac = datos == null ? null : datos.getVacunacion();
            Desparasitacion desp = datos == null ? null : datos.getDesparasitacion();
            VacunaVista vacunaVista = new VacunaVista(
                    vac == null || vac.getNombreVacuna() == null ? "" : vac.getNombreVacuna(),
                    (vac == null || vac.getFechaAplicacion() == null ? fechaCita : vac.getFechaAplicacion()).toString(),
                    vac == null || vac.getFechaRefuerzo() == null ? "" : vac.getFechaRefuerzo().toString(),
                    vac == null || vac.getNumeroLote() == null ? "" : vac.getNumeroLote(),
                    vac == null || vac.getNumeroDosis() == null ? "1" : String.valueOf(vac.getNumeroDosis()),
                    vac == null || vac.getObservaciones() == null ? "" : vac.getObservaciones());
            DesparasitacionVista despVista = new DesparasitacionVista(
                    desp == null || desp.getMarca() == null ? "" : desp.getMarca(),
                    (desp == null || desp.getFechaAplicacion() == null ? fechaCita : desp.getFechaAplicacion()).toString(),
                    desp == null || desp.getNumeroLote() == null ? "" : desp.getNumeroLote(),
                    desp == null || desp.getObservaciones() == null ? "" : desp.getObservaciones());
            TipoDesparasitacion tipoDesp = desp == null || desp.getTipo() == null ? TipoDesparasitacion.EXTERNA : desp.getTipo();
            List<OpcionVista> opDesparasitacion = List.of(
                    new OpcionVista("externa", TipoDesparasitacion.EXTERNA.name(), "Externa", tipoDesp == TipoDesparasitacion.EXTERNA),
                    new OpcionVista("interna", TipoDesparasitacion.INTERNA.name(), "Interna", tipoDesp == TipoDesparasitacion.INTERNA),
                    new OpcionVista("mixta", TipoDesparasitacion.MIXTA.name(), "Mixta", tipoDesp == TipoDesparasitacion.MIXTA));

            return new BitacoraVista(
                    agendamientoId, actual.getId(),
                    "#YZ-" + actual.getFechaAgendamiento().toString().replace("-", "") + "-" + String.format("%03d", agendamientoId),
                    nombre, nombre.substring(0, 1).toUpperCase(),
                    m.getFotoMascota() == null ? "" : m.getFotoMascota().trim(),
                    m.getSexo() == null ? "" : (m.getSexo().name().equals("MACHO") ? "♂" : "♀"),
                    m.getTipo() == null ? "—" : (m.getTipo().name().equals("GATO") ? "Gato" : "Perro"),
                    m.getRaza() == null || m.getRaza().isBlank() ? "—" : m.getRaza(),
                    edad(m.getFechaNacimiento(), hoy).isEmpty() ? "—" : edad(m.getFechaNacimiento(), hoy),
                    u == null ? "" : ((u.getNombre() == null ? "" : u.getNombre()) + " "
                            + (u.getApellido() == null ? "" : u.getApellido())).trim(),
                    fechaLarga(actual.getFechaAgendamiento()) + " • " + hora(hora.getHour() * 60 + hora.getMinute()),
                    pestanas, opServicios, opBano, opHallazgos,
                    obs[3], obs[1], obs[2],
                    datos == null || datos.getFotoSalida() == null ? "" : datos.getFotoSalida(),
                    datos == null || datos.getPrecioSubtotal() == null ? "" : miles(datos.getPrecioSubtotal()),
                    vacunaVista, despVista, opDesparasitacion, hoy.toString(),
                    MonedaColombiana.normalizar(otras).longValue(), pesos(total), listas, pestanas.size(),
                    completada, !completada, url(vista, fecha, ""));
        } catch (RuntimeException e) {
            LOG.warn("No se pudo abrir la bitácora del agendamiento " + agendamientoId + ": " + e.getMessage());
            return null;
        }
    }

    // Arma el texto de "observaciones_peluqueria":
    //   En la mascota se encontró: Parásitos, Suciedad.
    //   Observaciones: ...
    //   Recomendaciones: ...
    private static String componerObservaciones(List<String> hallazgos, String otro, String observaciones,
                                                String recomendaciones) {
        List<String> encontrados = new ArrayList<>();
        Set<String> marcados = hallazgos == null ? Set.of() : new HashSet<>(hallazgos);
        for (String h : HALLAZGOS) {
            if (marcados.contains(h)) {
                encontrados.add(h);
            }
        }
        if (marcados.contains(OTRA)) {
            String texto = linea(otro);
            encontrados.add(texto.isEmpty() ? OTRA : OTRA + ": " + texto);
        }
        List<String> partes = new ArrayList<>();
        if (!encontrados.isEmpty()) {
            partes.add(PRE_ENCONTRO + String.join(", ", encontrados) + ".");
        }
        if (!linea(observaciones).isEmpty()) {
            partes.add(PRE_OBSERVACIONES + linea(observaciones));
        }
        if (!linea(recomendaciones).isEmpty()) {
            partes.add(PRE_RECOMENDACIONES + linea(recomendaciones));
        }
        return partes.isEmpty() ? null : String.join("\n", partes);
    }

    // Separa el texto guardado en: [0] lo encontrado, [1] observaciones, [2] recomendaciones, [3] texto de "Otra"
    private static String[] separarObservaciones(String texto) {
        String[] r = {"", "", "", ""};
        if (texto == null || texto.isBlank()) {
            return r;
        }
        StringBuilder sueltas = new StringBuilder();
        for (String l : texto.split("\\R")) {
            if (l.startsWith(PRE_ENCONTRO)) {
                String resto = l.substring(PRE_ENCONTRO.length()).trim();
                if (resto.endsWith(".")) {
                    resto = resto.substring(0, resto.length() - 1);
                }
                int otra = resto.startsWith(OTRA) ? 0 : resto.indexOf(", " + OTRA);
                if (otra >= 0) {
                    String desdeOtra = resto.substring(otra == 0 ? 0 : otra + 2);
                    r[3] = desdeOtra.startsWith(OTRA + ": ") ? desdeOtra.substring(OTRA.length() + 2) : "";
                    resto = resto.substring(0, otra) + (otra == 0 ? OTRA : ", " + OTRA);
                }
                r[0] = resto;
            } else if (l.startsWith(PRE_OBSERVACIONES)) {
                r[1] = l.substring(PRE_OBSERVACIONES.length()).trim();
            } else if (l.startsWith(PRE_RECOMENDACIONES)) {
                r[2] = l.substring(PRE_RECOMENDACIONES.length()).trim();
            } else if (!l.isBlank()) {
                sueltas.append(sueltas.length() == 0 ? "" : " ").append(l.trim());   // texto antiguo sin formato
            }
        }
        if (sueltas.length() > 0) {
            r[1] = r[1].isEmpty() ? sueltas.toString() : sueltas + " " + r[1];
        }
        return r;
    }

    private static Set<String> hallazgosMarcados(String encontrado) {
        Set<String> r = new HashSet<>();
        if (encontrado == null || encontrado.isBlank()) {
            return r;
        }
        for (String parte : encontrado.split(",")) {
            r.add(parte.trim());
        }
        return r;
    }

    // Servicios marcados en el texto de la bitácora (tolera textos antiguos como "Baño y corte")
    private static List<String> serviciosMarcados(String tipoServicio) {
        List<String> r = new ArrayList<>();
        if (tipoServicio == null) {
            return r;
        }
        String t = tipoServicio.toLowerCase(COLOMBIA);
        for (String s : BitacoraServicioService.SERVICIOS) {
            if (t.contains(s.toLowerCase(COLOMBIA))) {
                r.add(s);
            }
        }
        return r;
    }

    private static TipoBano tipoBanoDe(String texto) {
        try {
            return texto == null || texto.isBlank() ? null : TipoBano.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // "45.000" | "45000" | "$ 45.000" -> 45000 (los pesos no llevan centavos; la coma se toma como decimal)
    private static BigDecimal leerPrecio(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.replaceAll("[^0-9,]", "").replace(",", ".");
        try {
            return limpio.isEmpty() ? null : new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static TipoDesparasitacion tipoDesparasitacionDe(String texto) {
        try {
            return texto == null || texto.isBlank() ? null
                    : TipoDesparasitacion.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static LocalDate leerFecha(String texto) {
        try {
            return texto == null || texto.isBlank() ? null : LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Integer leerEntero(String texto) {
        try {
            return texto == null || texto.isBlank() ? null : Integer.valueOf(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Texto limpio o null si viene vacío
    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    // Si el servicio se marcó, avisa con un mensaje claro cuando una fecha escrita no es válida
    private static void validarFechaFormulario(boolean marcado, String aplicacion, String refuerzo, String de) {
        if (!marcado) {
            return;
        }
        if (aplicacion != null && !aplicacion.isBlank() && leerFecha(aplicacion) == null) {
            throw new BusinessRuleException("La fecha de aplicación " + de + " no es válida.");
        }
        if (refuerzo != null && !refuerzo.isBlank() && leerFecha(refuerzo) == null) {
            throw new BusinessRuleException("La fecha de refuerzo " + de + " no es válida.");
        }
    }

    private static String linea(String texto) {
        return texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
    }

    private static String nombreDe(Mascota m) {
        return m == null || m.getNombre() == null || m.getNombre().isBlank() ? "Mascota" : m.getNombre();
    }

    // 45000 -> "45.000"
    private static String miles(BigDecimal valor) {
        NumberFormat f = NumberFormat.getNumberInstance(COLOMBIA);
        f.setMaximumFractionDigits(0);
        return f.format(MonedaColombiana.normalizar(valor));
    }

    // 45000 -> "$ 45.000"
    private static String pesos(BigDecimal valor) {
        return "$ " + miles(valor);
    }

    // ===================== Utilidades =====================

    private static String vistaValida(String vista) {
        return "dia".equals(vista) || "mes".equals(vista) ? vista : "semana";
    }

    private static LocalDate fechaOHoy(String texto) {
        try {
            if (texto != null && !texto.isBlank()) {
                return LocalDate.parse(texto);
            }
        } catch (DateTimeParseException ignored) {
            // fecha inválida: se usa hoy
        }
        return LocalDate.now(RelojColombia.reloj());
    }

    private static LocalDate lunesDe(LocalDate f) {
        return f.minusDays(f.getDayOfWeek().getValue() - 1L);
    }

    private static LocalDate desplazar(String vista, LocalDate f, int dir) {
        return switch (vista) {
            case "dia" -> f.plusDays(dir);
            case "mes" -> f.withDayOfMonth(1).plusMonths(dir);
            default -> f.plusWeeks(dir);
        };
    }

    private static String url(String vista, LocalDate fecha, String extra) {
        return "/admin/citas?vista=" + vista + "&fecha=" + fecha + extra;
    }

    private static String periodo(String vista, LocalDate f) {
        if ("dia".equals(vista)) {
            return DIAS_LARGO[f.getDayOfWeek().getValue() - 1] + " " + f.getDayOfMonth() + " "
                    + MESES_CORTO[f.getMonthValue() - 1] + " " + f.getYear();
        }
        if ("mes".equals(vista)) {
            return MESES[f.getMonthValue() - 1] + " " + f.getYear();
        }
        LocalDate l = lunesDe(f);
        LocalDate d = l.plusDays(6);
        return l.getMonthValue() == d.getMonthValue()
                ? MESES[l.getMonthValue() - 1] + " " + l.getYear()
                : MESES_CORTO[l.getMonthValue() - 1] + " – " + MESES_CORTO[d.getMonthValue() - 1] + " " + d.getYear();
    }

    private static String fechaLarga(LocalDate d) {
        return DIAS_LARGO[d.getDayOfWeek().getValue() - 1] + " " + d.getDayOfMonth() + " de "
                + MESES[d.getMonthValue() - 1].toLowerCase();
    }

    private static String edad(LocalDate nacimiento, LocalDate hoy) {
        if (nacimiento == null || nacimiento.isAfter(hoy)) {
            return "";
        }
        Period p = Period.between(nacimiento, hoy);
        if (p.getYears() >= 1) {
            return p.getYears() + (p.getYears() == 1 ? " año" : " años");
        }
        int meses = p.getMonths();
        return meses <= 0 ? "Menos de 1 mes" : meses + (meses == 1 ? " mes" : " meses");
    }

    private static String unir(List<String> nombres) {
        if (nombres.size() <= 1) {
            return nombres.isEmpty() ? "" : nombres.get(0);
        }
        return String.join(", ", nombres.subList(0, nombres.size() - 1)) + " y " + nombres.get(nombres.size() - 1);
    }

    private static double redondear(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static String sufijo(int min) {
        return (min / 60) % 24 >= 12 ? "PM" : "AM";
    }

    private static String h12(int min) {
        int h = (min / 60) % 24;
        return (h % 12 == 0 ? 12 : h % 12) + ":" + String.format("%02d", min % 60);
    }

    private static String hora(int min) {
        return h12(min) + " " + sufijo(min);
    }

    private static String rango(int ini, int fin) {
        return sufijo(ini).equals(sufijo(fin))
                ? h12(ini) + " – " + h12(fin) + " " + sufijo(fin)
                : hora(ini) + " – " + hora(fin);
    }
}