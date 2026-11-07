package usta.veterinaria.infrastructure.adapter.out.persistence.mapper;

import usta.veterinaria.domain.model.Producto;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.ProductoEntity;

// Convierte entre Producto y ProductoEntity
public final class ProductoEntityMapper {

    private ProductoEntityMapper() {
    }

    // Dominio -> entidad
    public static ProductoEntity toEntity(Producto producto) {
        if (producto == null) {
            return null;
        }

        ProductoEntity entity = new ProductoEntity();
        entity.setId(producto.getId());
        entity.setNombre(producto.getNombre());
        entity.setMarca(producto.getMarca());
        entity.setCategoria(producto.getCategoria());
        entity.setDescripcion(producto.getDescripcion());
        entity.setTipo(producto.getTipo());
        entity.setPrecioBase(producto.getPrecioBase());
        entity.setPrecioVenta(producto.getPrecioVenta());
        entity.setFotoProducto(producto.getFotoProducto());
        entity.setStock(producto.getStock());
        return entity;
    }

    // Entidad -> dominio
    public static Producto toDomain(ProductoEntity entity) {
        if (entity == null) {
            return null;
        }

        return Producto.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .marca(entity.getMarca())
                .categoria(entity.getCategoria())
                .descripcion(entity.getDescripcion())
                .tipo(entity.getTipo())
                .precioBase(entity.getPrecioBase())
                .precioVenta(entity.getPrecioVenta())
                .fotoProducto(entity.getFotoProducto())
                .stock(entity.getStock())
                .build();
    }
}