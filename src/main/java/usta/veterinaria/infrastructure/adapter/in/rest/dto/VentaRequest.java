package usta.veterinaria.infrastructure.adapter.in.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

// Datos que llegan para registrar una venta que el administrador ya hizo en persona.
// Si no se envía la fecha se toma la de hoy
public record VentaRequest(
        @Schema(example = "2026-11-08")
        LocalDate fechaVenta,

        @NotEmpty(message = "La venta debe tener al menos un producto") List<@Valid DetalleVentaRequest> detalles) {
}
