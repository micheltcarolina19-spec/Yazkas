package usta.veterinaria.infrastructure.adapter.in.web.usuario;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateData;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import usta.veterinaria.application.port.in.admin.GestionarBitacoraServicioUseCase;
import usta.veterinaria.application.port.in.mascota.GestionarMascotasUseCase;
import usta.veterinaria.application.port.in.usuario.GestionarCitasUseCase;
import usta.veterinaria.domain.model.admin.BitacoraServicio;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.domain.model.usuario.EstadoCita;
import usta.veterinaria.domain.model.usuario.MascotaCita;
import usta.veterinaria.domain.service.RelojColombia;
import usta.veterinaria.infrastructure.adapter.in.web.ImagenStorage;
import usta.veterinaria.infrastructure.adapter.in.web.sesion.SesionActual;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Historial del cliente (navbar del cliente -> Historial).
 * Plantilla: src/main/resources/templates/usuarios/historialCitas.html
 *
 * GET /historial[?mascota=ID]                -> próximas citas e historial de cada mascota del usuario
 * GET /historial/mascotas/{id}/bitacora.pdf  -> bitácora de la ÚLTIMA cita completada de esa mascota
 *                                              (la que llenó el admin al finalizar: servicios, precio, foto, observaciones...)
 *
 * El usuario sale de la sesión y solo puede ver / descargar lo de SUS mascotas.
 * En la página no se muestra precio; el precio solo va en el PDF de la bitácora.
 */
@Path("/historial")
public class HistorialClienteResource {

    private static final Logger LOG = Logger.getLogger(HistorialClienteResource.class);
    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter HORA_12 = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", COLOMBIA);
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("d MMM yyyy", COLOMBIA);
    private static final String SERVICIO_CLIENTE = "Baño y Peluquería";
    private static final String RAIZ_ESTATICOS = "META-INF/resources/";
    private static final int MAX_IMAGEN = 5 * 1024 * 1024;

    @Inject
    @Location("usuarios/historialCitas.html")
    Template historialCitas;

    @Inject
    SesionActual sesion;

    @Inject
    GestionarMascotasUseCase mascotas;

    @Inject
    GestionarCitasUseCase citas;

    @Inject
    GestionarBitacoraServicioUseCase bitacoras;

    @Inject
    ImagenStorage imagenes;

    // ------------------------------------------------------------------ Datos que pinta la plantilla

    // Una fila de cita (próxima o del historial)
    @TemplateData
    public record CitaFila(Long agendamientoId, String fecha, String fechaCorta, String dia, String mes,
                           String hora, String estado, String estadoClase, String servicios,
                           String companeros, String duracion, boolean conBitacora) {
    }

    // Resumen de la última cita completada con bitácora (sin precio)
    @TemplateData
    public record UltimaVista(String fecha, String hora, String servicios, String foto, boolean conVacuna,
                              boolean conDesparasitacion) {
    }

    @TemplateData
    public record MascotaHistorial(Long id, String nombre, String raza, String tipo, String foto, String inicial,
                                   List<CitaFila> proximas, List<CitaFila> pasadas, UltimaVista ultima,
                                   int completadas, int canceladas) {
    }

    // Cita completada de una mascota con su bitácora
    private record CitaConBitacora(MascotaCita cita, BitacoraServicio bitacora) {
    }

    // ------------------------------------------------------------------ Página

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response pagina(@QueryParam("mascota") Long mascotaSeleccionada) {
        if (!sesion.isActiva()) {
            return redirigir("/login");
        }
        if (!sesion.isCliente()) {
            return redirigir(sesion.getRutaPerfil());
        }

        List<Mascota> lista = mascotas.listarPorUsuario(sesion.getUsuarioId());
        LocalDate hoy = LocalDate.now(RelojColombia.reloj());

        // Citas de cada mascota
        Map<Long, List<MascotaCita>> citasPorMascota = new LinkedHashMap<>();
        for (Mascota m : lista) {
            List<MascotaCita> propias = new ArrayList<>();
            for (MascotaCita c : citas.listarCitasPorMascota(m.getId())) {
                if (c.getAgendamientoCita() == null || c.getFechaAgendamiento() == null) {
                    continue;
                }
                if (c.getMascota() == null || c.getMascota().getNombre() == null) {
                    c.setMascota(m);
                }
                propias.add(c);
            }
            citasPorMascota.put(m.getId(), propias);
        }

        // Mascotas que van juntas en cada agendamiento (para mostrar "con Luna")
        Map<Long, Set<String>> nombresPorAgendamiento = new LinkedHashMap<>();
        citasPorMascota.values().forEach(listaCitas -> listaCitas.forEach(c ->
                nombresPorAgendamiento.computeIfAbsent(c.getAgendamientoCita().getId(), k -> new java.util.LinkedHashSet<>())
                        .add(c.getMascota().getNombre())));

        // Bitácoras de todas las citas completadas, en una sola consulta
        Map<Long, BitacoraServicio> bitacoraPorCita = bitacorasDe(citasPorMascota.values().stream()
                .flatMap(List::stream).toList());

        List<MascotaHistorial> vistas = new ArrayList<>();
        for (Mascota m : lista) {
            List<MascotaCita> propias = citasPorMascota.get(m.getId());
            List<CitaFila> proximas = new ArrayList<>();
            List<CitaFila> pasadas = new ArrayList<>();
            int completadas = 0;
            int canceladas = 0;

            List<MascotaCita> ordenadas = propias.stream()
                    .sorted(Comparator.comparing(MascotaCita::getFechaAgendamiento)
                            .thenComparing(c -> c.getHoraAgendamiento() == null ? LocalTime.MIN : c.getHoraAgendamiento()))
                    .toList();
            for (MascotaCita c : ordenadas) {
                EstadoCita estado = c.getAgendamientoCita().getEstado();
                if (estado == EstadoCita.COMPLETADA) {
                    completadas++;
                } else if (estado == EstadoCita.CANCELADA) {
                    canceladas++;
                }
                CitaFila fila = fila(c, m.getNombre(), nombresPorAgendamiento, bitacoraPorCita.get(c.getId()));
                boolean activa = estado == EstadoCita.PROGRAMADA || estado == EstadoCita.REPROGRAMADA;
                if (activa && !c.getFechaAgendamiento().isBefore(hoy)) {
                    proximas.add(fila);
                } else {
                    pasadas.add(0, fila);   // historial: la más reciente primero
                }
            }

            CitaConBitacora ultima = ultimaConBitacora(propias, bitacoraPorCita);
            UltimaVista ultimaVista = ultima == null ? null : new UltimaVista(
                    fechaTexto(ultima.cita().getFechaAgendamiento()),
                    hora(ultima.cita().getHoraAgendamiento()),
                    servicios(ultima.bitacora()),
                    fotoPublica(ultima.bitacora().getFotoSalida()),
                    ultima.bitacora().getVacunacion() != null,
                    ultima.bitacora().getDesparasitacion() != null);

            vistas.add(new MascotaHistorial(m.getId(), m.getNombre(),
                    m.getRaza() == null ? "" : m.getRaza(),
                    m.getTipo() == null ? "" : m.getTipo().name(),
                    fotoPublica(m.getFotoMascota()),
                    inicial(m.getNombre()),
                    proximas, pasadas, ultimaVista, completadas, canceladas));
        }

        Long seleccionada = vistas.stream().map(MascotaHistorial::id)
                .filter(id -> id.equals(mascotaSeleccionada)).findFirst()
                .orElse(vistas.isEmpty() ? null : vistas.get(0).id());

        return Response.ok(historialCitas
                .data("mascotas", vistas)
                .data("seleccionada", seleccionada)).build();
    }

    // ------------------------------------------------------------------ Descargar bitácora (PDF)

    @GET
    @Path("/mascotas/{id: \\d+}/bitacora.pdf")
    @Produces("application/pdf")
    public Response descargarBitacora(@PathParam("id") Long mascotaId) {
        if (!sesion.isActiva()) {
            return redirigir("/login");
        }
        if (!sesion.isCliente()) {
            return redirigir(sesion.getRutaPerfil());
        }

        // Solo mascotas del usuario en sesión
        Mascota mascota = mascotas.listarPorUsuario(sesion.getUsuarioId()).stream()
                .filter(m -> m.getId().equals(mascotaId)).findFirst().orElse(null);
        if (mascota == null) {
            return Response.status(Response.Status.NOT_FOUND).type(MediaType.TEXT_PLAIN)
                    .entity("Esta mascota no pertenece a tu cuenta").build();
        }

        List<MascotaCita> propias = citas.listarCitasPorMascota(mascotaId).stream()
                .filter(c -> c.getAgendamientoCita() != null && c.getFechaAgendamiento() != null)
                .toList();
        CitaConBitacora ultima = ultimaConBitacora(propias, bitacorasDe(propias));
        if (ultima == null) {
            return Response.status(Response.Status.NOT_FOUND).type(MediaType.TEXT_PLAIN)
                    .entity(mascota.getNombre() + " aún no tiene una cita finalizada con bitácora").build();
        }
        if (ultima.cita().getMascota() == null || ultima.cita().getMascota().getNombre() == null) {
            ultima.cita().setMascota(mascota);
        }

        byte[] pdf = BitacoraCitaPdf.generar(
                mascota,
                ultima.cita(),
                ultima.bitacora(),
                leerImagen(ultima.bitacora().getFotoSalida()),
                leerImagen(mascota.getFotoMascota()));

        String archivo = "bitacora-" + slug(mascota.getNombre()) + "-" + ultima.cita().getFechaAgendamiento() + ".pdf";
        return Response.ok(pdf)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo + "\"")
                .build();
    }

    // ------------------------------------------------------------------ Apoyo

    // Bitácora (con precio, es decir, ya llenada por el admin) de cada cita completada; clave = id de MascotaCita
    private Map<Long, BitacoraServicio> bitacorasDe(List<MascotaCita> lista) {
        Set<Long> completadas = new HashSet<>();
        Set<Long> idsCitas = new HashSet<>();
        for (MascotaCita c : lista) {
            if (c.getAgendamientoCita() != null && c.getAgendamientoCita().getEstado() == EstadoCita.COMPLETADA) {
                completadas.add(c.getAgendamientoCita().getId());
                idsCitas.add(c.getId());
            }
        }
        Map<Long, BitacoraServicio> r = new LinkedHashMap<>();
        if (completadas.isEmpty()) {
            return r;
        }
        for (BitacoraServicio b : bitacoras.listarPorAgendamientos(completadas)) {
            if (b == null || b.getMascotaCita() == null || !idsCitas.contains(b.getMascotaCita().getId())) {
                continue;
            }
            if (b.getPrecioSubtotal() != null && b.getPrecioSubtotal().signum() > 0) {
                r.put(b.getMascotaCita().getId(), b);
            }
        }
        return r;
    }

    // La cita completada más reciente que tiene bitácora
    private static CitaConBitacora ultimaConBitacora(List<MascotaCita> lista, Map<Long, BitacoraServicio> bitacoraPorCita) {
        return lista.stream()
                .filter(c -> c.getAgendamientoCita().getEstado() == EstadoCita.COMPLETADA)
                .filter(c -> bitacoraPorCita.containsKey(c.getId()))
                .max(Comparator.comparing(MascotaCita::getFechaAgendamiento)
                        .thenComparing(c -> c.getHoraAgendamiento() == null ? LocalTime.MIN : c.getHoraAgendamiento()))
                .map(c -> new CitaConBitacora(c, bitacoraPorCita.get(c.getId())))
                .orElse(null);
    }

    private static CitaFila fila(MascotaCita c, String nombre, Map<Long, Set<String>> nombresPorAgendamiento,
                                 BitacoraServicio bitacora) {
        EstadoCita estado = c.getAgendamientoCita().getEstado();
        String companeros = nombresPorAgendamiento.getOrDefault(c.getAgendamientoCita().getId(), Set.of()).stream()
                .filter(Objects::nonNull)
                .filter(n -> !n.equals(nombre))
                .collect(Collectors.joining(" y "));
        LocalDate f = c.getFechaAgendamiento();
        String mes = f.format(DateTimeFormatter.ofPattern("MMM", COLOMBIA)).replace(".", "");
        return new CitaFila(
                c.getAgendamientoCita().getId(),
                fechaTexto(f),
                f.format(FECHA_CORTA).replace(".", ""),
                String.valueOf(f.getDayOfMonth()),
                mes.substring(0, 1).toUpperCase(COLOMBIA) + mes.substring(1),
                hora(c.getHoraAgendamiento()),
                textoEstado(estado),
                estado == null ? "" : estado.name().toLowerCase(COLOMBIA),
                bitacora == null ? SERVICIO_CLIENTE : servicios(bitacora),
                companeros,
                CitasClienteResource.textoDuracion(c.getDuracionCita()),
                bitacora != null);
    }

    private static String servicios(BitacoraServicio b) {
        return b == null || b.getTipoServicio() == null || b.getTipoServicio().isBlank()
                ? SERVICIO_CLIENTE : b.getTipoServicio().trim();
    }

    private static String textoEstado(EstadoCita estado) {
        if (estado == null) {
            return "Sin estado";
        }
        return switch (estado) {
            case PROGRAMADA -> "Programada";
            case REPROGRAMADA -> "Reprogramada";
            case COMPLETADA -> "Completada";
            case CANCELADA -> "Cancelada";
        };
    }

    // Ruta o URL de la foto tal como está guardada (la plantilla la usa en <img src>)
    private static String fotoPublica(String foto) {
        return foto == null ? "" : foto.trim();
    }

    // Bytes de una imagen para el PDF: data URI, /uploads/{carpeta}/{nombre}, estático de la app o URL http(s).
    // Si no se puede leer, el PDF sale con "Sin foto".
    private byte[] leerImagen(String origen) {
        if (origen == null || origen.isBlank()) {
            return null;
        }
        String valor = origen.trim();
        try {
            if (valor.startsWith("data:")) {
                int coma = valor.indexOf(',');
                return coma < 0 ? null : Base64.getMimeDecoder().decode(valor.substring(coma + 1));
            }
            if (valor.startsWith(ImagenStorage.RAIZ_URL)) {
                String[] partes = valor.substring(ImagenStorage.RAIZ_URL.length()).split("/");
                if (partes.length != 2) {
                    return null;
                }
                var ruta = imagenes.ubicar(partes[0], partes[1]);
                return ruta.isPresent() ? Files.readAllBytes(ruta.get()) : null;
            }
            if (valor.startsWith("http://") || valor.startsWith("https://")) {
                return descargar(valor);
            }
            if (valor.contains("..")) {
                return null;
            }
            try (InputStream in = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(RAIZ_ESTATICOS + valor.replaceFirst("^/+", ""))) {
                return in == null ? null : in.readNBytes(MAX_IMAGEN);
            }
        } catch (Exception e) {
            LOG.debugf("No se pudo leer la imagen para la bitácora: %s", e.getMessage());
            return null;
        }
    }

    // Foto pegada como URL por el admin: tiempo y tamaño limitados
    private static byte[] descargar(String url) throws Exception {
        HttpClient cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(6))
                .header("Accept", "image/*")
                .GET().build();
        HttpResponse<InputStream> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream in = respuesta.body()) {
            if (respuesta.statusCode() != 200) {
                return null;
            }
            byte[] bytes = in.readNBytes(MAX_IMAGEN + 1);
            return bytes.length > MAX_IMAGEN ? null : bytes;
        }
    }

    private static String hora(LocalTime hora) {
        return hora == null ? "" : hora.format(HORA_12);
    }

    private static String fechaTexto(LocalDate dia) {
        String t = dia.format(FECHA_LARGA);
        return t.substring(0, 1).toUpperCase(COLOMBIA) + t.substring(1);
    }

    private static String inicial(String nombre) {
        return nombre == null || nombre.isBlank() ? "M" : nombre.trim().substring(0, 1).toUpperCase(COLOMBIA);
    }

    private static String slug(String texto) {
        if (texto == null || texto.isBlank()) {
            return "mascota";
        }
        String s = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return s.isEmpty() ? "mascota" : s;
    }

    private Response redirigir(String ruta) {
        return Response.seeOther(URI.create(ruta)).build();
    }
}
