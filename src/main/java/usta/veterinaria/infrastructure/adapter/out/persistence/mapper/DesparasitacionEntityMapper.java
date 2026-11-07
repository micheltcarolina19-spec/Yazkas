package usta.veterinaria.infrastructure.adapter.out.persistence.mapper;

import usta.veterinaria.domain.model.Desparasitacion;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.DesparasitacionEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.MascotaEntity;

// Convierte entre Desparasitacion y DesparasitacionEntity
public final class DesparasitacionEntityMapper {

    private DesparasitacionEntityMapper() {
    }

    // Dominio -> entidad
    public static DesparasitacionEntity toEntity(Desparasitacion desparasitacion) {
        if (desparasitacion == null) {
            return null;
        }

        DesparasitacionEntity entity = new DesparasitacionEntity();
        entity.setId(desparasitacion.getId());
        entity.setMarca(desparasitacion.getMarca());
        entity.setFechaAplicacion(desparasitacion.getFechaAplicacion());
        entity.setTipo(desparasitacion.getTipo());
        entity.setNumeroLote(desparasitacion.getNumeroLote());
        entity.setObservaciones(desparasitacion.getObservaciones());

        if (desparasitacion.getMascota() != null) {
            MascotaEntity mascota = new MascotaEntity();
            mascota.setId(desparasitacion.getMascota().getId());
            entity.setMascota(mascota);
        }

        return entity;
    }

    // Entidad -> dominio
    public static Desparasitacion toDomain(DesparasitacionEntity entity) {
        if (entity == null) {
            return null;
        }

        return Desparasitacion.builder()
                .id(entity.getId())
                .marca(entity.getMarca())
                .fechaAplicacion(entity.getFechaAplicacion())
                .tipo(entity.getTipo())
                .numeroLote(entity.getNumeroLote())
                .observaciones(entity.getObservaciones())
                .mascota(
                        MascotaEntityMapper.toDomain(entity.getMascota())
                )
                .build();
    }
}