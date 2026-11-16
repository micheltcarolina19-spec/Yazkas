package usta.veterinaria.infrastructure.adapter.in.web.usuario;


import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class    PaginaResource {

    @Inject
    @Location("usuarios/index.html")   // ruta dentro de src/main/resources/templates/
    Template index;

    @Inject
    @Location("usuarios/agregarMascota.html")
    Template agregarMascota;

    @Inject
    @Location("usuarios/perfilMascota.html")
    Template perfilMascota;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance inicio() {
        return index.instance();
    }

    // Formulario para registrar una mascota. Mientras no exista login, el dueño llega por
    // ?usuarioId= y el formulario lo usa para llamar a POST /api/usuarios/{usuarioId}/mascotas
    @GET
    @Path("mascotas/agregar")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance agregarMascota(@QueryParam("usuarioId") Long usuarioId) {
        return agregarMascota.data("usuarioId", usuarioId);
    }

    // Perfil de una mascota: /mascotas/perfil?id=5
    // Los datos se cargan en el navegador con GET /api/mascotas/{id} y GET /api/usuarios/{usuarioId}
    @GET
    @Path("mascotas/perfil")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance perfilMascota(@QueryParam("id") Long id) {
        return perfilMascota.data("mascotaId", id);
    }
}