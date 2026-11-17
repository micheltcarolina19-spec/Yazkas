package usta.veterinaria.infrastructure.adapter.in.web.usuario;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;

@Path("/")
public class PaginaResource {

    @Inject
    @Location("usuarios/index.html")   // ruta dentro de src/main/resources/templates/
    Template index;

    // Home (página pública). Al cerrar sesión, /logout borra la cookie y vuelve aquí como visitante.
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response inicio() {
        return Response.ok(index.instance()).build();
    }

    // "Agregar mascota" lo atiende AgregarMascotaResource (GET /mascotas/agregar),
    // que toma el dueño de la sesión en lugar de ?usuarioId=.

    // Enlace antiguo /mascotas/perfil?id=5: redirige a /mascotas/{id} de PerfilMascotaResource,
    // que valida que la mascota sea del usuario en sesión.
    @GET
    @Path("mascotas/perfil")
    public Response perfilMascota(@QueryParam("id") Long id) {
        return Response.seeOther(URI.create(id == null ? "/mascotas" : "/mascotas/" + id)).build();
    }
}