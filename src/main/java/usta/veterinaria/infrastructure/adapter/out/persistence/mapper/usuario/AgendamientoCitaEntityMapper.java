package usta.veterinaria.infrastructure.adapter.out.persistence.mapper.usuario;

import usta.veterinaria.domain.model.usuario.AgendamientoCita;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.usuario.AgendamientoCitaEntity;

// Convierte entre AgendamientoCita y AgendamientoCitaEntity
public final class AgendamientoCitaEntityMapper {

    private AgendamientoCitaEntityMapper() {
    }

    // Dominio -> entidad
    public static AgendamientoCitaEntity toEntity(AgendamientoCita agendamiento) {
        if (agendamiento == null) {
            return null;
        }

        AgendamientoCitaEntity entity = new AgendamientoCitaEntity();
        entity.setId(agendamiento.getId());
        entity.setPrecioTotal(agendamiento.getPrecioTotal());
        entity.setEstado(agendamiento.getEstado());

        return entity;
    }

    // Entidad -> dominio
    public static AgendamientoCita toDomain(AgendamientoCitaEntity entity) {
        if (entity == null) {
            return null;
        }

        return AgendamientoCita.builder()
                .id(entity.getId())
                .precioTotal(entity.getPrecioTotal())
                .estado(entity.getEstado())
                .build();
    }
}