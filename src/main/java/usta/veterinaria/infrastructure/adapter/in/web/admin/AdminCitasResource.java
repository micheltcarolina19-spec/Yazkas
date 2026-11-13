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
import usta.veterinaria.application.port.in.usuario.GestionarCitasUseCase;
import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.domain.model.usuario.AgendamientoCita;
import usta.veterinaria.domain.model.usuario.EstadoCita;
import usta.veterinaria.domain.model.usuario.MascotaCita;
import usta.veterinaria.domain.model.usuario.Usuario;
import usta.veterinaria.domain.service.RelojColombia;
import usta.veterinaria.domain.service.usuario.CitaService;
import usta.veterinaria.infrastructure.adapter.in.web.admin.CalendarioVista.AccionVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.CalendarioVista.BloqueVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.CalendarioVista.CeldaVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.CalendarioVista.ChipVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.CalendarioVista.DiaVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.CalendarioVista.MascotaVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.CalendarioVista.ResumenVista;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Adaptador de entrada WEB: calendario de citas del administrador (renderizado en el servidor).
//   - GET  /admin/citas?vista=dia|semana|mes&fecha=2026-11-12   -> calendario
//   - GET  /admin/citas?...&reprogramar=ID | &cancelar=ID        -> calendario con el diálogo abierto
//   - POST /admin/citas/reprogramar | /cancelar                  -> ejecuta la acción y redirige (303)
// Usa el MISMO caso de uso que el adaptador REST (GestionarCitasUseCase).
@Path("/admin/citas")
@ApplicationScoped
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

    @Inject
    GestionarCitasUseCase citas;

    // El dominio decide cuántos minutos bloquea cada duración (BAJA, MEDIA, ALTA)
    private final CitaService citaService = new CitaService();

    // ===================== Páginas =====================

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance mostrar(@QueryParam("vista") String vista,
                                    @QueryParam("fecha") String fecha,
                                    @QueryParam("reprogramar") Long reprogramar,
                                    @QueryParam("cancelar") Long cancelar,
                                    @QueryParam("ok") String ok) {
        return pagina(vistaValida(vista), fechaOHoy(fecha), ok, null, reprogramar, cancelar);
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

    private Response redirigir(String vista, LocalDate fecha, String mensaje) {
        String destino = url(vista, fecha, "&ok=" + URLEncoder.encode(mensaje, StandardCharsets.UTF_8));
        return Response.seeOther(URI.create(destino)).build();
    }

    private Response conError(String vista, LocalDate fecha, Long reprogramarId, Long cancelarId, String error) {
        return Response.ok(pagina(vista, fecha, null, error, reprogramarId, cancelarId).render())
                .type(MediaType.TEXT_HTML).build();
    }

    // ===================== Armado de la página =====================

    private TemplateInstance pagina(String vista, LocalDate fecha, String ok, String error,
                                    Long reprogramarId, Long cancelarId) {
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
                .data("cancelar", accion(cancelarId, vista, fecha));

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
        String categoria = categoriaDe(g.agendamiento);
        List<MascotaVista> mascotas = g.citas.stream().map(c -> vistaMascota(c, hoy)).toList();
        boolean modificable = g.agendamiento != null && g.agendamiento.getId() != null
                && (estado == EstadoCita.PROGRAMADA || estado == EstadoCita.REPROGRAMADA);
        Long id = g.agendamiento == null ? null : g.agendamiento.getId();

        return new BloqueVista(categoria, servicioTexto(categoria), estadoClase(estado), estadoTexto(estado),
                unir(mascotas.stream().map(MascotaVista::nombre).toList()),
                rango(g.ini, g.fin), fechaLarga(g.fecha),
                top, alto, izquierda, ancho, lado, vertical, modificable, mascotas.size() > 1,
                mascotas,
                modificable ? url(vista, fechaVista, "&reprogramar=" + id) : "",
                modificable ? url(vista, fechaVista, "&cancelar=" + id) : "");
    }

    private MascotaVista vistaMascota(MascotaCita cita, LocalDate hoy) {
        Mascota m = cita.getMascota();
        Usuario u = m.getUsuario();
        String nombre = m.getNombre() == null || m.getNombre().isBlank() ? "Mascota" : m.getNombre();
        String duenio = u == null ? "" : ((u.getNombre() == null ? "" : u.getNombre()) + " "
                + (u.getApellido() == null ? "" : u.getApellido())).trim();
        long horas = citaService.minutosReservados(cita.getDuracionCita()) / 60;
        return new MascotaVista(
                nombre,
                m.getRaza() == null ? "" : m.getRaza(),
                edad(m.getFechaNacimiento(), hoy),
                m.getFotoMascota() == null ? "" : m.getFotoMascota().trim(),
                nombre.substring(0, 1).toUpperCase(),
                duenio,
                u == null || u.getCelular() == null ? "" : u.getCelular(),
                "Reserva de " + horas + " h");
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
                String categoria = categoriaDe(g.agendamiento);
                chips.add(new ChipVista(hora(g.ini),
                        unir(g.citas.stream().map(c -> c.getMascota().getNombre() == null ? "Mascota" : c.getMascota().getNombre()).toList()),
                        categoria, estadoClase(g.estado()), url("dia", d, "")));
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

    // PUNTO A CONECTAR: hoy la cita no guarda qué servicio es (baño, peluquería u otro),
    // así que todas salen como peluquería. Cuando exista el dato, se decide aquí: "bano" | "pelu" | "otros".
    private static String categoriaDe(AgendamientoCita agendamiento) {
        return "pelu";
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