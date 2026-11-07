package usta.veterinaria.infrastructure.adapter.out.persistence.mapper;

import usta.veterinaria.domain.model.DetalleVenta;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.DetalleVentaEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.ProductoEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.VentaEntity;

// Convierte entre DetalleVenta y DetalleVentaEntity
public final class DetalleVentaEntityMapper {

    private DetalleVentaEntityMapper() {
    }

    // Dominio -> entidad
    public static DetalleVentaEntity toEntity(DetalleVenta detalle) {
        if (detalle == null) {
            return null;
        }

        DetalleVentaEntity entity = new DetalleVentaEntity();
        entity.setId(detalle.getId());
        entity.setSubtotal(detalle.getSubtotal());
        entity.setCantidad(detalle.getCantidad());

        if (detalle.getVenta() != null) {
            VentaEntity venta = new VentaEntity();
            venta.setId(detalle.getVenta().getId());
            entity.setVenta(venta);
        }

        if (detalle.getProducto() != null) {
            ProductoEntity producto = new ProductoEntity();
            producto.setId(detalle.getProducto().getId());
            entity.setProducto(producto);
        }

        return entity;
    }

    // Entidad -> dominio
    public static DetalleVenta toDomain(DetalleVentaEntity entity) {
        if (entity == null) {
            return null;
        }

        return DetalleVenta.builder()
                .id(entity.getId())
                .subtotal(entity.getSubtotal())
                .cantidad(entity.getCantidad())
                .venta(
                        VentaEntityMapper.toDomain(entity.getVenta())
                )
                .producto(
                        ProductoEntityMapper.toDomain(entity.getProducto())
                )
                .build();
    }
}