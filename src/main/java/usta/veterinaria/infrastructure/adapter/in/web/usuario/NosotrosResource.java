package usta.veterinaria.infrastructure.web;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Vista pública "Nosotros": misión, visión, servicios, horarios y ubicación.
 * Plantilla: src/main/resources/templates/usuarios/nosotros.html
 */
@Path("/nosotros")
public class NosotrosResource {

    @Inject
    @Location("usuarios/nosotros")
    Template nosotros;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance ver() {
        return nosotros.instance();
    }
}