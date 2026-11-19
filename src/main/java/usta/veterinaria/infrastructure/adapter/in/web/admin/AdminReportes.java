package usta.veterinaria.infrastructure.adapter.in.web.admin;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateData;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import usta.veterinaria.application.exception.ResourceNotFoundException;
import usta.veterinaria.application.port.in.mascota.GestionarHistoriaClinicaUseCase;
import usta.veterinaria.application.port.in.mascota.GestionarMascotasUseCase;
import usta.veterinaria.application.port.in.mascota.GestionarSaludMascotaUseCase;
import usta.veterinaria.application.port.in.usuario.GestionarUsuariosUseCase;
import usta.veterinaria.domain.model.mascota.HistoriaClinica;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.domain.model.usuario.RolUsuario;
import usta.veterinaria.domain.model.usuario.Usuario;
import usta.veterinaria.infrastructure.adapter.in.rest.resource.admin.HistorialMascotaPdf;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

// import jakarta.annotation.security.RolesAllowed;   // <- descomentar cuando exista seguridad (login)

import usta.veterinaria.infrastructure.adapter.in.web.ImagenStorage;
import usta.veterinaria.infrastructure.adapter.in.web.sesion.SoloAdmin;

@Path("/admin/reportes")
@SoloAdmin
public class AdminReportes {

    @Inject
    @Location("admin/adminReportes.html")
    Template index;


    @Inject
    ImagenStorage imagenes;

    @Inject
    GestionarUsuariosUseCase usuarios;

    @Inject
    GestionarMascotasUseCase mascotas;

    @Inject
    GestionarSaludMascotaUseCase salud;

    @Inject
    GestionarHistoriaClinicaUseCase historias;

    // Página: totales y todas las mascotas con su propietario
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance reportes() {
        long totalClientes = usuarios.listar().stream()
                .filter(u -> u.getRol() == RolUsuario.CLIENTE)
                .count();

        List<FilaReporte> filas = mascotas.listar().stream()
                .map(FilaReporte::de)
                .sorted(Comparator.comparing(FilaReporte::mascota, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return index.data("totalClientes", totalClientes)
                .data("totalMascotas", filas.size())
                .data("filas", filas);
    }

    // Descarga el historial clínico (ficha, historia, vacunas y desparasitaciones) de una mascota
    @GET
    @Path("/mascotas/{id}/historial.pdf")
    @Produces("application/pdf")
    public Response descargarHistorial(@PathParam("id") Long id) {
        Mascota mascota = mascotas.buscarPorId(id);

        byte[] pdf = HistorialMascotaPdf.generar(
                mascota,
                historiaOpcional(mascota.getId()),
                salud.listarVacunaciones(mascota.getId()),
                salud.listarDesparasitaciones(mascota.getId()),
                imagenes.leer(mascota.getFotoMascota()).orElse(null));

        return Response.ok(pdf)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"historial-" + slug(mascota.getNombre()) + ".pdf\"")
                .build();
    }

    // La historia clínica es opcional: una mascota nueva puede no tenerla
    private HistoriaClinica historiaOpcional(Long mascotaId) {
        try {
            return historias.buscarPorMascota(mascotaId);
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }

    private static String slug(String texto) {
        String base = Normalizer.normalize(texto == null ? "mascota" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return base.isEmpty() ? "mascota" : base;
    }

    // Fila que consume la plantilla; "busqueda" es el texto normalizado para el buscador del navegador
    @TemplateData
    public record FilaReporte(Long id, String mascota, String tipo, String raza,
                              String propietario, String celular, String correo, String busqueda) {

        static FilaReporte de(Mascota m) {
            Usuario u = m.getUsuario();
            String propietario = u == null ? "-" : (u.getNombre() + " " + valor(u.getApellido())).trim();
            String tipo = m.getTipo() == null ? "-" : m.getTipo().name();
            return new FilaReporte(
                    m.getId(), valor(m.getNombre()), tipo, valor(m.getRaza()), propietario,
                    u == null ? "-" : valor(u.getCelular()),
                    u == null ? "-" : valor(u.getCorreo()),
                    normalizar(m.getNombre() + " " + propietario));
        }

        private static String valor(String texto) {
            return texto == null || texto.isBlank() ? "-" : texto;
        }

        private static String normalizar(String texto) {
            return Normalizer.normalize(texto, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .toLowerCase(Locale.ROOT);
        }
    }
}