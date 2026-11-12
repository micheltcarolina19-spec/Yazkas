package usta.veterinaria.infrastructure.adapter.out.persistence.mapper.admin;

import usta.veterinaria.domain.model.admin.PlanillaSanidad;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.admin.PlanillaSanidadEntity;

// Convierte entre el modelo de dominio PlanillaSanidad y la entidad JPA PlanillaSanidadEntity
public final class PlanillaSanidadEntityMapper {

    private PlanillaSanidadEntityMapper() {
    }

    public static PlanillaSanidadEntity toEntity(PlanillaSanidad planilla) {
        PlanillaSanidadEntity entity = new PlanillaSanidadEntity();
        entity.setId(planilla.getId());
        entity.setFechaPlanilla(planilla.getFechaPlanilla());
        entity.setDiaSanidad(planilla.getDiaSanidad());
        entity.setAreaLimpieza(planilla.getAreaLimpieza());
        entity.setHerramienta(planilla.getHerramienta());
        entity.setLimpiezaRealizada(planilla.getLimpiezaRealizada());
        entity.setDesinfeccion(planilla.getDesinfeccion());
        entity.setObservaciones(planilla.getObservaciones());
        return entity;
    }

    public static PlanillaSanidad toDomain(PlanillaSanidadEntity entity) {
        return PlanillaSanidad.builder()
                .id(entity.getId())
                .fechaPlanilla(entity.getFechaPlanilla())
                .diaSanidad(entity.getDiaSanidad())
                .areaLimpieza(entity.getAreaLimpieza())
                .herramienta(entity.getHerramienta())
                .limpiezaRealizada(entity.getLimpiezaRealizada())
                .desinfeccion(entity.getDesinfeccion())
                .observaciones(entity.getObservaciones())
                .build();
    }
}