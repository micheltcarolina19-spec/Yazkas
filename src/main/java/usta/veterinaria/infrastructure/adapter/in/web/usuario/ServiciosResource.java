package usta.veterinaria.infrastructure.adapter.in.web.usuario;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Vista pública "Servicios" (navbar público -> Servicios). Solo informativa:
 * baño, peluquería, desparasitación y vacunación.
 * Plantilla: src/main/resources/templates/usuarios/servicios.html
 */
@Path("/servicios")
public class ServiciosResource {

    @Inject
    @Location("usuarios/servicios")
    Template servicios;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance ver() {
        return servicios.instance();
    }
}
