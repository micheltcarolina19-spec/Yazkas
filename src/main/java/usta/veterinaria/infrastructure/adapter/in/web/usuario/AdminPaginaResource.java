package usta.veterinaria.infrastructure.adapter.in.web.usuario;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import usta.veterinaria.application.port.in.admin.ConsultarFinanzasUseCase;

import java.time.LocalDate;

@Path("/admin")
public class AdminPaginaResource {

    @Inject
    @Location("admin/indexAdmin.html")
    Template index;

    @Inject
    ConsultarFinanzasUseCase finanzas;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance dashboard() {
        LocalDate fin = LocalDate.now();
        LocalDate inicio = fin.withDayOfMonth(1);
        return index
                .data("ingresos", finanzas.ingresosTotales(inicio, fin))
                .data("topProductos", finanzas.productosMasVendidos(inicio, fin, 5));
    }
}