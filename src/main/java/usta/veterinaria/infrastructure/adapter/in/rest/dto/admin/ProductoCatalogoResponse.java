package usta.veterinaria.infrastructure.adapter.in.rest.dto.admin;

// Datos que se devuelven de un producto en el catálogo público (vista del CLIENTE: SIN precios)
public record ProductoCatalogoResponse(
        Long id,
        String nombre,
        String marca,
        String categoria,
        String descripcion,
        String tipo,
        String fotoProducto,
        Integer stock) {
}
