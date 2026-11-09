package usta.veterinaria.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Datos que se devuelven de una venta con sus líneas (valores en pesos colombianos)
public record VentaResponse(
        Long id,
        LocalDate fechaVenta,
        BigDecimal totalVenta,
        List<DetalleVentaResponse> detalles) {
}
