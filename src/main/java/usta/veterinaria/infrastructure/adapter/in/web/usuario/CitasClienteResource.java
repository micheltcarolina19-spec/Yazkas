package usta.veterinaria.infrastructure.adapter.in.web.usuario;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateData;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import usta.veterinaria.application.port.in.mascota.GestionarMascotasUseCase;
import usta.veterinaria.application.port.in.usuario.GestionarCitasUseCase;
import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.domain.model.usuario.DuracionCita;
import usta.veterinaria.domain.model.usuario.EstadoCita;
import usta.veterinaria.domain.model.usuario.MascotaCita;
import usta.veterinaria.domain.service.RelojColombia;
import usta.veterinaria.domain.service.usuario.CitaService;
import usta.veterinaria.infrastructure.adapter.in.web.sesion.SesionActual;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * "Agenda tu cita" del cliente (navbar del cliente -> Citas).
 * Plantilla: src/main/resources/templates/usuarios/agendarCita.html
 *
 * GET  /citas                                      -> la página (mascotas del usuario + sus próximas citas)
 * GET  /citas/disponibilidad?fecha=..&mascotas=..  -> duración estimada por la IA y horarios libres (JSON)
 * POST /citas                                      -> agenda la cita (JSON: mascotaIds, fecha, hora)
 *
 * El usuario sale de la sesión. Al cliente NO se le muestra precio.
 * Reglas (CitaService): atención 8:30-17:30 y máximo 2 mascotas al mismo tiempo, de uno o varios dueños.
 */
@Path("/citas")
public class CitasClienteResource {

    private static final int MAXIMO_MASCOTAS_POR_CITA = 2;
    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter HORA_12 = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", COLOMBIA);

    @Inject
    @Location("usuarios/agendarCita.html")
    Template agendarCita;

    @Inject
    SesionActual sesion;

    @Inject
    GestionarMascotasUseCase mascotas;

    @Inject
    GestionarCitasUseCase citas;

    // Datos que pinta la plantilla
    @TemplateData
    public record MascotaVista(Long id, String nombre, String raza, String tipo, String foto, String inicial) {
    }

    @TemplateData
    public record CitaVista(String mascotas, String fecha, String hora, String estado) {
    }

    // ------------------------------------------------------------------ Página

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response pagina() {
        if (!sesion.isActiva()) {
            return redirigir("/login");
        }
        if (!sesion.isCliente()) {
            return redirigir(sesion.getRutaPerfil());
        }

        List<Mascota> lista = mascotas.listarPorUsuario(sesion.getUsuarioId());
        List<MascotaVista> vistas = lista.stream()
                .map(m -> new MascotaVista(m.getId(), m.getNombre(),
                        m.getRaza() == null ? "" : m.getRaza(),
                        m.getTipo() == null ? "" : m.getTipo().name(),
                        m.getFotoMascota() == null ? "" : m.getFotoMascota().trim(),
                        inicial(m.getNombre())))
                .toList();

        return Response.ok(agendarCita
                .data("mascotas", vistas)
                .data("proximas", proximasCitas(lista))
                .data("hoy", LocalDate.now(RelojColombia.reloj()).toString())
                .data("maximoMascotas", MAXIMO_MASCOTAS_POR_CITA)).build();
    }

    // ------------------------------------------------------------------ Disponibilidad (JSON)

    @GET
    @Path("/disponibilidad")
    @Produces(MediaType.APPLICATION_JSON)
    public Response disponibilidad(@QueryParam("fecha") String fecha, @QueryParam("mascotas") List<Long> mascotaIds) {
        Response sinPermiso = validarClienteJson();
        if (sinPermiso != null) {
            return sinPermiso;
        }
        LocalDate dia = leerFecha(fecha);
        List<Long> ids = validarMascotas(mascotaIds);

        Map<Long, DuracionCita> duraciones = citas.estimarDuraciones(sesion.getUsuarioId(), ids);
        Map<LocalTime, String> horarios = citas.disponibilidad(sesion.getUsuarioId(), ids, dia);

        List<Map<String, Object>> listaDuraciones = new ArrayList<>();
        duraciones.forEach((id, d) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("mascotaId", id);
            item.put("duracion", d.name());
            item.put("texto", textoDuracion(d));
            listaDuraciones.add(item);
        });

        List<Map<String, Object>> listaHorarios = new ArrayList<>();
        horarios.forEach((hora, motivo) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("hora", hora.toString());
            item.put("etiqueta", hora.format(HORA_12));
            item.put("disponible", motivo == null);
            item.put("motivo", motivo == null ? null : textoMotivo(motivo));
            listaHorarios.add(item);
        });

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("fecha", dia.toString());
        respuesta.put("fechaTexto", fechaTexto(dia));
        respuesta.put("duraciones", listaDuraciones);
        respuesta.put("horarios", listaHorarios);
        return Response.ok(respuesta).build();
    }

    // ------------------------------------------------------------------ Agendar (JSON)

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response agendar(Map<String, Object> body) {
        Response sinPermiso = validarClienteJson();
        if (sinPermiso != null) {
            return sinPermiso;
        }
        if (body == null) {
            throw new BusinessRuleException("Faltan los datos de la cita");
        }

        List<Long> ids = new ArrayList<>();
        if (body.get("mascotaIds") instanceof List<?> lista) {
            for (Object o : lista) {
                if (o != null) {
                    ids.add(Long.valueOf(o.toString()));
                }
            }
        }
        ids = validarMascotas(ids);
        LocalDate dia = leerFecha(body.get("fecha") == null ? null : body.get("fecha").toString());
        LocalTime hora = leerHora(body.get("hora") == null ? null : body.get("hora").toString());

        List<MascotaCita> creadas = citas.agendar(sesion.getUsuarioId(), ids, dia, hora);

        List<Map<String, Object>> detalle = new ArrayList<>();
        for (MascotaCita c : creadas) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("mascota", c.getMascota() == null ? "" : c.getMascota().getNombre());
            item.put("duracion", textoDuracion(c.getDuracionCita()));
            detalle.add(item);
        }
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("agendamientoId", creadas.isEmpty() || creadas.get(0).getAgendamientoCita() == null
                ? null : creadas.get(0).getAgendamientoCita().getId());
        respuesta.put("fechaTexto", fechaTexto(dia));
        respuesta.put("hora", hora.format(HORA_12));
        respuesta.put("citas", detalle);
        return Response.status(Response.Status.CREATED).entity(respuesta).build();
    }

    // ------------------------------------------------------------------ Apoyo

    // Próximas citas (programadas o reprogramadas) de las mascotas del usuario, una fila por agendamiento
    private List<CitaVista> proximasCitas(List<Mascota> lista) {
        LocalDate hoy = LocalDate.now(RelojColombia.reloj());
        Map<Long, List<MascotaCita>> porAgendamiento = new LinkedHashMap<>();
        for (Mascota m : lista) {
            for (MascotaCita c : citas.listarCitasPorMascota(m.getId())) {
                EstadoCita estado = c.getAgendamientoCita() == null ? null : c.getAgendamientoCita().getEstado();
                boolean activa = estado == EstadoCita.PROGRAMADA || estado == EstadoCita.REPROGRAMADA;
                if (activa && c.getFechaAgendamiento() != null && !c.getFechaAgendamiento().isBefore(hoy)) {
                    if (c.getMascota() == null || c.getMascota().getNombre() == null) {
                        c.setMascota(m);
                    }
                    porAgendamiento.computeIfAbsent(c.getAgendamientoCita().getId(), k -> new ArrayList<>()).add(c);
                }
            }
        }
        return porAgendamiento.values().stream()
                .map(grupo -> grupo.get(0))
                .sorted(Comparator.comparing(MascotaCita::getFechaAgendamiento)
                        .thenComparing(MascotaCita::getHoraAgendamiento))
                .limit(5)
                .map(primera -> {
                    List<String> nombres = porAgendamiento.get(primera.getAgendamientoCita().getId()).stream()
                            .map(c -> c.getMascota().getNombre()).filter(Objects::nonNull).distinct().toList();
                    return new CitaVista(String.join(" y ", nombres),
                            fechaTexto(primera.getFechaAgendamiento()),
                            primera.getHoraAgendamiento().format(HORA_12),
                            primera.getAgendamientoCita().getEstado() == EstadoCita.REPROGRAMADA ? "Reprogramada" : "Programada");
                })
                .toList();
    }

    private List<Long> validarMascotas(List<Long> ids) {
        List<Long> limpios = ids == null ? List.of() : ids.stream().filter(Objects::nonNull).distinct().toList();
        if (limpios.isEmpty()) {
            throw new BusinessRuleException("Selecciona al menos una mascota");
        }
        if (limpios.size() > MAXIMO_MASCOTAS_POR_CITA) {
            throw new BusinessRuleException("Puedes agendar máximo " + MAXIMO_MASCOTAS_POR_CITA + " mascotas en el mismo horario");
        }
        return limpios;
    }

    private static LocalDate leerFecha(String texto) {
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BusinessRuleException("Selecciona una fecha válida");
        }
    }

    private static LocalTime leerHora(String texto) {
        try {
            return LocalTime.parse(texto);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BusinessRuleException("Selecciona un horario válido");
        }
    }

    // La duración se muestra sin precio
    static String textoDuracion(DuracionCita d) {
        if (d == null) {
            return "";
        }
        return switch (d) {
            case BAJA -> "Corta · hasta 2 horas";
            case MEDIA -> "Media · de 2 a 4 horas";
            case ALTA -> "Larga · más de 4 horas";
        };
    }

    private static String textoMotivo(String motivo) {
        return switch (motivo) {
            case CitaService.NO_DISPONIBLE_PASADO -> "Ya pasó";
            case CitaService.NO_DISPONIBLE_CIERRE -> "No alcanza antes del cierre";
            case CitaService.NO_DISPONIBLE_LLENO -> "Sin cupo";
            case CitaService.NO_DISPONIBLE_MASCOTA -> "Tu mascota ya tiene cita";
            default -> "No disponible";
        };
    }

    private static String fechaTexto(LocalDate dia) {
        String t = dia.format(FECHA_LARGA);
        return t.substring(0, 1).toUpperCase(COLOMBIA) + t.substring(1);
    }

    private static String inicial(String nombre) {
        return nombre == null || nombre.isBlank() ? "M" : nombre.trim().substring(0, 1).toUpperCase(COLOMBIA);
    }

    // null = puede continuar
    private Response validarClienteJson() {
        if (!sesion.isActiva() || !sesion.isCliente()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("mensaje", "Tu sesión expiró. Vuelve a iniciar sesión."))
                    .type(MediaType.APPLICATION_JSON).build();
        }
        return null;
    }

    private Response redirigir(String ruta) {
        return Response.seeOther(URI.create(ruta)).build();
    }
}
