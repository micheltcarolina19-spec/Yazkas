package usta.veterinaria.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;

// Datos que se devuelven de un producto (vista del ADMINISTRADOR: incluye precios y stock)
public record ProductoResponse(
        Long id,
        String nombre,
        String marca,
        String categoria,
        String descripcion,
        String tipo,
        BigDecimal precioBase,
        BigDecimal precioVenta,
        String fotoProducto,
        Integer stock) {
}
