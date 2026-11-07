package usta.veterinaria.infrastructure.adapter.out.persistence.mapper;

import usta.veterinaria.domain.model.PlanillaSanidad;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.PlanillaSanidadEntity;

// Convierte entre PlanillaSanidad y PlanillaSanidadEntity
public final class PlanillaSanidadEntityMapper {

    private PlanillaSanidadEntityMapper() {
    }

    // Dominio -> entidad
    public static PlanillaSanidadEntity toEntity(PlanillaSanidad planilla) {
        if (planilla == null) {
            return null;
        }

        PlanillaSanidadEntity entity = new PlanillaSanidadEntity();
        entity.setId(planilla.getId());
        entity.setFechaPlanilla(planilla.getFechaPlanilla());
        entity.setDiaSanidad(planilla.getDiaSanidad());
        entity.setAreaLimpieza(planilla.getAreaLimpieza());
        entity.setHerramienta(planilla.getHerramienta());
        entity.setLimpiezaRealizada(planilla.getLimpiezaRealizada());
        entity.setDesinfeccion(planilla.getDesinfeccion());
        return entity;
    }

    // Entidad -> dominio
    public static PlanillaSanidad toDomain(PlanillaSanidadEntity entity) {
        if (entity == null) {
            return null;
        }

        return PlanillaSanidad.builder()
                .id(entity.getId())
                .fechaPlanilla(entity.getFechaPlanilla())
                .diaSanidad(entity.getDiaSanidad())
                .areaLimpieza(entity.getAreaLimpieza())
                .herramienta(entity.getHerramienta())
                .limpiezaRealizada(entity.getLimpiezaRealizada())
                .desinfeccion(entity.getDesinfeccion())
                .build();
    }
}