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
 * Vista pública "Contáctanos": botones rápidos, administradoras, dirección, horario y mapa.
 * Plantilla: src/main/resources/templates/usuarios/contacto.html
 */
@Path("/contacto")
public class ContactoResource {

    @Inject
    @Location("usuarios/contacto")
    Template contacto;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance ver() {
        return contacto.instance();
    }
}
