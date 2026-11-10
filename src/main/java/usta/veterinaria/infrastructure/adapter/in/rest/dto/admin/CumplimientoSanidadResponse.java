package usta.veterinaria.infrastructure.adapter.in.rest.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Porcentaje de registros con limpieza y desinfección completas en un rango de fechas (ej: 80.00)
public record CumplimientoSanidadResponse(
        LocalDate inicio,
        LocalDate fin,
        BigDecimal porcentajeCumplimiento) {
    // Datos que llegan para registrar una venta que el administrador ya hizo en persona.
    // Si no se envía la fecha se toma la de hoy
    public static record VentaRequest(
            @Schema(example = "2026-11-08")
            LocalDate fechaVenta,

            @NotEmpty(message = "La venta debe tener al menos un producto") List<@Valid DetalleVentaRequest> detalles) {
    }

    // Datos que se devuelven de una venta con sus líneas (valores en pesos colombianos)
    public static record VentaResponse(
            Long id,
            LocalDate fechaVenta,
            BigDecimal totalVenta,
            List<DetalleVentaResponse> detalles) {
    }
}
