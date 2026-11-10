package usta.veterinaria.infrastructure.adapter.out.persistence.mapper.admin;

import usta.veterinaria.domain.model.admin.Venta;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.admin.VentaEntity;

// Convierte entre Venta y VentaEntity
public final class VentaEntityMapper {

    private VentaEntityMapper() {
    }

    // Dominio -> entidad
    public static VentaEntity toEntity(Venta venta) {
        if (venta == null) {
            return null;
        }

        VentaEntity entity = new VentaEntity();
        entity.setId(venta.getId());
        entity.setFechaVenta(venta.getFechaVenta());
        entity.setTotalVenta(venta.getTotalVenta());

        return entity;
    }

    // Entidad -> dominio
    public static Venta toDomain(VentaEntity entity) {
        if (entity == null) {
            return null;
        }

        return Venta.builder()
                .id(entity.getId())
                .fechaVenta(entity.getFechaVenta())
                .totalVenta(entity.getTotalVenta())
                .build();
    }
}