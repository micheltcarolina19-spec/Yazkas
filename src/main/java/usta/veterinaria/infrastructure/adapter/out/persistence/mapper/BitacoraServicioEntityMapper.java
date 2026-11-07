package usta.veterinaria.infrastructure.adapter.out.persistence.mapper;

import usta.veterinaria.domain.model.BitacoraServicio;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.AgendamientoCitaEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.BitacoraServicioEntity;

// Convierte entre BitacoraServicio y BitacoraServicioEntity
public final class BitacoraServicioEntityMapper {

    private BitacoraServicioEntityMapper() {
    }

    // Dominio -> entidad
    public static BitacoraServicioEntity toEntity(BitacoraServicio bitacora) {
        if (bitacora == null) {
            return null;
        }

        BitacoraServicioEntity entity = new BitacoraServicioEntity();
        entity.setId(bitacora.getId());
        entity.setTipoServicio(bitacora.getTipoServicio());
        entity.setFotoSalida(bitacora.getFotoSalida());
        entity.setTipoBano(bitacora.getTipoBano());
        entity.setPrecioSubtotal(bitacora.getPrecioSubtotal());
        entity.setObservacionesPeluqueria(bitacora.getObservacionesPeluqueria());

        if (bitacora.getAgendamientoCita() != null) {
            AgendamientoCitaEntity agendamiento = new AgendamientoCitaEntity();
            agendamiento.setId(bitacora.getAgendamientoCita().getId());
            entity.setAgendamientoCita(agendamiento);
        }

        return entity;
    }

    // Entidad -> dominio
    public static BitacoraServicio toDomain(BitacoraServicioEntity entity) {
        if (entity == null) {
            return null;
        }

        return BitacoraServicio.builder()
                .id(entity.getId())
                .tipoServicio(entity.getTipoServicio())
                .fotoSalida(entity.getFotoSalida())
                .tipoBano(entity.getTipoBano())
                .precioSubtotal(entity.getPrecioSubtotal())
                .observacionesPeluqueria(entity.getObservacionesPeluqueria())
                .agendamientoCita(
                        AgendamientoCitaEntityMapper.toDomain(entity.getAgendamientoCita())
                )
                .build();
    }
}