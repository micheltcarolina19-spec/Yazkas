package usta.veterinaria.infrastructure.adapter.in.web.admin;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import usta.veterinaria.infrastructure.adapter.in.web.sesion.SoloAdmin;

// Página de la planilla de sanidad: /admin/planilla-sanidad
// Solo entrega el HTML; los datos los pide el JS a /api/planilla-sanidad
@Path("/admin/planilla-sanidad")
@SoloAdmin

public class AdminPlanillaSanidadResource {

    @Inject
    @Location("admin/adminPlanillaSanidad.html")
    Template index;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        return index.instance();
    }
}