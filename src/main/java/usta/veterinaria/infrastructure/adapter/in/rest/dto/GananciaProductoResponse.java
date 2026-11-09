package usta.veterinaria.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;

// Ganancia por unidad de un producto (precio de venta - precio base) en pesos colombianos
public record GananciaProductoResponse(
        Long productoId,
        BigDecimal gananciaUnitaria) {
}
