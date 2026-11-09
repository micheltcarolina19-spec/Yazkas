package usta.veterinaria.infrastructure.adapter.in.rest.resource;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import usta.veterinaria.application.port.in.ConsultarFinanzasUseCase;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.ResumenFinancieroResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.FinanzasRestMapper;

// import jakarta.annotation.security.RolesAllowed;   // <- descomentar cuando exista seguridad (login)

import java.time.LocalDate;

// Adaptador de entrada REST para el panel de finanzas (todo en pesos colombianos)
@Path("/api/finanzas")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Finanzas", description = "Panel de ingresos, gastos y utilidad")
// @RolesAllowed("ADMIN")   // <- protege TODOS los endpoints de esta clase
public class FinanzasResource {

    @Inject
    ConsultarFinanzasUseCase finanzas;

    // Resumen del panel: /api/finanzas/resumen?inicio=2026-11-01&fin=2026-11-30
    @GET
    @Path("/resumen")
    @Operation(summary = "Consultar el resumen financiero de un rango de fechas")
    public ResumenFinancieroResponse resumen(@QueryParam("inicio")
                                             @NotNull(message = "La fecha inicial es obligatoria") LocalDate inicio,
                                             @QueryParam("fin")
                                             @NotNull(message = "La fecha final es obligatoria") LocalDate fin) {
        return FinanzasRestMapper.toResumenResponse(
                inicio,
                fin,
                finanzas.ingresosPorVentas(inicio, fin),
                finanzas.ingresosPorCitas(inicio, fin),
                finanzas.ingresosTotales(inicio, fin),
                finanzas.totalGastos(inicio, fin),
                finanzas.utilidad(inicio, fin),
                finanzas.margenUtilidad(inicio, fin),
                finanzas.gananciaProductos(inicio, fin),
                finanzas.gastosPorCategoria(inicio, fin),
                finanzas.ventasPorCategoriaProducto(inicio, fin));
    }
}
