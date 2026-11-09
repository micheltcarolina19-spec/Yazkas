package usta.veterinaria.infrastructure.adapter.in.rest.mapper;

import usta.veterinaria.domain.model.PlanillaSanidad;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.CumplimientoSanidadResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.PlanillaSanidadResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Convierte la planilla de sanidad del dominio a la respuesta REST
public final class PlanillaSanidadRestMapper {

    private PlanillaSanidadRestMapper() {
    }

    public static PlanillaSanidadResponse toResponse(PlanillaSanidad planilla) {
        return new PlanillaSanidadResponse(
                planilla.getId(),
                planilla.getFechaPlanilla(),
                MapperUtils.nombre(planilla.getDiaSanidad()),
                MapperUtils.nombre(planilla.getAreaLimpieza()),
                MapperUtils.nombre(planilla.getHerramienta()),
                planilla.getLimpiezaRealizada(),
                planilla.getDesinfeccion());
    }

    public static List<PlanillaSanidadResponse> toResponseList(List<PlanillaSanidad> planillas) {
        return MapperUtils.lista(planillas, PlanillaSanidadRestMapper::toResponse);
    }

    // Arma la respuesta con el porcentaje que calculó el caso de uso
    public static CumplimientoSanidadResponse toCumplimientoResponse(LocalDate inicio, LocalDate fin,
                                                                     BigDecimal porcentajeCumplimiento) {
        return new CumplimientoSanidadResponse(inicio, fin, porcentajeCumplimiento);
    }
}
