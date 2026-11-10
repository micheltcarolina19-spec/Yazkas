package usta.veterinaria.infrastructure.adapter.in.rest.dto.admin;

import java.math.BigDecimal;

// Datos que se devuelven de una línea de la venta
public record DetalleVentaResponse(
        Long id,
        Long productoId,
        String nombreProducto,
        Integer cantidad,
        BigDecimal subtotal) {
}
