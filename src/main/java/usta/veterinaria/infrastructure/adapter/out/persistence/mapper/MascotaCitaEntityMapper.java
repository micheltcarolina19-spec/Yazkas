package usta.veterinaria.infrastructure.adapter.out.persistence.mapper;

import usta.veterinaria.domain.model.MascotaCita;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.AgendamientoCitaEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.MascotaCitaEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.MascotaEntity;

// Convierte entre MascotaCita y MascotaCitaEntity
public final class MascotaCitaEntityMapper {

    private MascotaCitaEntityMapper() {
    }

    // Dominio -> entidad
    public static MascotaCitaEntity toEntity(MascotaCita mascotaCita) {
        if (mascotaCita == null) {
            return null;
        }

        MascotaCitaEntity entity = new MascotaCitaEntity();
        entity.setId(mascotaCita.getId());
        entity.setFechaCreacion(mascotaCita.getFechaCreacion());
        entity.setFechaAgendamiento(mascotaCita.getFechaAgendamiento());
        entity.setHoraAgendamiento(mascotaCita.getHoraAgendamiento());
        entity.setDuracionCita(mascotaCita.getDuracionCita());

        if (mascotaCita.getMascota() != null) {
            MascotaEntity mascota = new MascotaEntity();
            mascota.setId(mascotaCita.getMascota().getId());
            entity.setMascota(mascota);
        }

        if (mascotaCita.getAgendamientoCita() != null) {
            AgendamientoCitaEntity agendamiento = new AgendamientoCitaEntity();
            agendamiento.setId(mascotaCita.getAgendamientoCita().getId());
            entity.setAgendamientoCita(agendamiento);
        }

        return entity;
    }

    // Entidad -> dominio
    public static MascotaCita toDomain(MascotaCitaEntity entity) {
        if (entity == null) {
            return null;
        }

        return MascotaCita.builder()
                .id(entity.getId())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaAgendamiento(entity.getFechaAgendamiento())
                .horaAgendamiento(entity.getHoraAgendamiento())
                .duracionCita(entity.getDuracionCita())
                .mascota(
                        MascotaEntityMapper.toDomain(entity.getMascota())
                )
                .agendamientoCita(
                        AgendamientoCitaEntityMapper.toDomain(entity.getAgendamientoCita())
                )
                .build();
    }
}