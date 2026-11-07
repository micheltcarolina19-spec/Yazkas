package usta.veterinaria.infrastructure.adapter.out.persistence.mapper;

import usta.veterinaria.domain.model.Vacunacion;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.MascotaEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.VacunacionEntity;

// Convierte entre Vacunacion y VacunacionEntity
public final class VacunacionEntityMapper {

    private VacunacionEntityMapper() {
    }

    // Dominio -> entidad (usa la MascotaEntity ya resuelta/gestionada por JPA)
    public static VacunacionEntity toEntity(Vacunacion vacunacion, MascotaEntity mascota) {
        if (vacunacion == null) {
            return null;
        }

        VacunacionEntity entity = new VacunacionEntity();
        entity.setId(vacunacion.getId());
        entity.setNombreVacuna(vacunacion.getNombreVacuna());
        entity.setFechaAplicacion(vacunacion.getFechaAplicacion());
        entity.setFechaRefuerzo(vacunacion.getFechaRefuerzo());
        entity.setNumeroLote(vacunacion.getNumeroLote());
        entity.setNumeroDosis(vacunacion.getNumeroDosis());
        entity.setObservaciones(vacunacion.getObservaciones());
        entity.setMascota(mascota);
        return entity;
    }

    // Entidad -> dominio
    public static Vacunacion toDomain(VacunacionEntity entity) {
        if (entity == null) {
            return null;
        }

        return Vacunacion.builder()
                .id(entity.getId())
                .nombreVacuna(entity.getNombreVacuna())
                .fechaAplicacion(entity.getFechaAplicacion())
                .fechaRefuerzo(entity.getFechaRefuerzo())
                .numeroLote(entity.getNumeroLote())
                .numeroDosis(entity.getNumeroDosis())
                .observaciones(entity.getObservaciones())
                .mascota(MascotaEntityMapper.toDomain(entity.getMascota()))
                .build();
    }
}