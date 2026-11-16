package usta.veterinaria.infrastructure.adapter.in.web.usuario;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

// Página web del catálogo de productos para el cliente: GET /catalogo
// Solo entrega la plantilla; los productos los carga catalogo.js desde /api/catalogo
// (sin precios ni stock).
@Path("/catalogo")
public class CatalogoResource {

    @Inject
    @Location("usuarios/catalogo.html")  // ruta dentro de src/main/resources/templates/
    Template catalogo;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance catalogo() {
        return catalogo.instance();
    }
}