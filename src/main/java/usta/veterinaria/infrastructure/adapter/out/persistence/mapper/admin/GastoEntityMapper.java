package usta.veterinaria.infrastructure.adapter.out.persistence.mapper.admin;

import usta.veterinaria.domain.model.admin.Gasto;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.admin.GastoEntity;

// Convierte entre Gasto y GastoEntity
public final class GastoEntityMapper {

    private GastoEntityMapper() {
    }

    // Dominio -> entidad
    public static GastoEntity toEntity(Gasto gasto) {
        if (gasto == null) {
            return null;
        }

        GastoEntity entity = new GastoEntity();
        entity.setId(gasto.getId());
        entity.setNombre(gasto.getNombre());
        entity.setMonto(gasto.getMonto());
        entity.setFechaGasto(gasto.getFechaGasto());
        entity.setCategoria(gasto.getCategoria());

        return entity;
    }

    // Entidad -> dominio
    public static Gasto toDomain(GastoEntity entity) {
        if (entity == null) {
            return null;
        }

        return Gasto.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .monto(entity.getMonto())
                .fechaGasto(entity.getFechaGasto())
                .categoria(entity.getCategoria())
                .build();
    }
}