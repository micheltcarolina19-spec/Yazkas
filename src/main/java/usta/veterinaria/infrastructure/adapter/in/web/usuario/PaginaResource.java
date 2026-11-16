package usta.veterinaria.infrastructure.adapter.in.web.usuario;


import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class    PaginaResource {

    @Inject
    @Location("usuarios/index.html")   // ruta dentro de src/main/resources/templates/
    Template index;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance inicio() {
        return index.instance();
    }
}