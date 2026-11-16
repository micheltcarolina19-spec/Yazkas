package usta.veterinaria.infrastructure.adapter.in.web.usuario;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import usta.veterinaria.application.port.in.mascota.GestionarMascotasUseCase;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.infrastructure.adapter.in.web.sesion.SesionActual;

import java.net.URI;
import java.util.List;

/**
 * Vista "Perfil de Mascota" del cliente.
 * Plantilla: src/main/resources/templates/usuarios/perfilMascota.html
 *
 * GET /mascotas       -> entrada desde "Mi perfil" del navbar:
 *                        si no tiene mascotas lo manda a agregar, si tiene, abre la primera.
 * GET /mascotas/{id}  -> perfil de una mascota (solo si es del usuario en sesión).
 */
@Path("/mascotas")
public class PerfilMascotaResource {

    @Inject
    @Location("usuarios/perfilMascota.html")
    Template perfilMascota;

    @Inject
    SesionActual sesion;

    // Caso de uso que ya existe en el proyecto: tiene listarPorUsuario(Long usuarioId)
    @Inject
    GestionarMascotasUseCase mascotas;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response miPerfil() {
        Response sinPermiso = validarCliente();
        if (sinPermiso != null) {
            return sinPermiso;
        }

        List<Mascota> lista = mascotas.listarPorUsuario(sesion.getUsuarioId());
        if (lista.isEmpty()) {
            return redirigir("/mascotas/agregar");
        }
        return redirigir("/mascotas/" + lista.get(0).getId());
    }

    @GET
    @Path("{id: \\d+}")
    @Produces(MediaType.TEXT_HTML)
    public Response ver(@PathParam("id") Long id) {
        Response sinPermiso = validarCliente();
        if (sinPermiso != null) {
            return sinPermiso;
        }

        // Evita que un cliente abra el perfil de una mascota que no es suya cambiando el id en la URL
        boolean esSuya = mascotas.listarPorUsuario(sesion.getUsuarioId())
                .stream()
                .anyMatch(m -> id.equals(m.getId()));
        if (!esSuya) {
            return redirigir("/mascotas");
        }

        return Response.ok(perfilMascota.data("mascotaId", id)).build();
    }

    // null = puede continuar; si no, la redirección correspondiente
    private Response validarCliente() {
        if (!sesion.isActiva()) {
            return redirigir("/login");
        }
        if (!sesion.isCliente()) {
            return redirigir(sesion.getRutaPerfil());
        }
        return null;
    }

    private Response redirigir(String ruta) {
        return Response.seeOther(URI.create(ruta)).build();
    }
}