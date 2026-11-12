package usta.veterinaria.infrastructure.adapter.in.rest.resource.admin;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import usta.veterinaria.application.port.in.admin.ConsultarFinanzasUseCase;
import usta.veterinaria.domain.service.admin.FinanzasService;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.admin.DetalleVentaRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.admin.FinanzasRestMapper;

// import jakarta.annotation.security.RolesAllowed;   // <- descomentar cuando exista seguridad (login)

import java.time.LocalDate;
import java.util.List;

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
    public DetalleVentaRequest.ResumenFinancieroResponse resumen(@QueryParam("inicio")
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

    // Ranking: /api/finanzas/productos-mas-vendidos?inicio=2026-11-01&fin=2026-11-30&limite=3
    @GET
    @Path("/productos-mas-vendidos")
    @Operation(summary = "Consultar los productos más vendidos de un rango de fechas")
    public List<FinanzasService.ProductoVendido> productosMasVendidos(
            @QueryParam("inicio") @NotNull(message = "La fecha inicial es obligatoria") LocalDate inicio,
            @QueryParam("fin") @NotNull(message = "La fecha final es obligatoria") LocalDate fin,
            @QueryParam("limite") @DefaultValue("3") int limite) {
        return finanzas.productosMasVendidos(inicio, fin, limite);
    }

    // Para las gráficas: /api/finanzas/serie?inicio=2026-11-01&fin=2026-11-30&agrupar=DIA   (DIA, SEMANA o MES)
    @GET
    @Path("/serie")
    @Operation(summary = "Consultar ingresos, gastos y ventas por día, semana o mes")
    public List<FinanzasService.PuntoSerie> serie(
            @QueryParam("inicio") @NotNull(message = "La fecha inicial es obligatoria") LocalDate inicio,
            @QueryParam("fin") @NotNull(message = "La fecha final es obligatoria") LocalDate fin,
            @QueryParam("agrupar") @DefaultValue("DIA") String agrupar) {
        return finanzas.serieFinanciera(inicio, fin, agrupar);
    }
}