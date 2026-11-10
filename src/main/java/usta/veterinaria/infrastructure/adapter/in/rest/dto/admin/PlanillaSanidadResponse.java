package usta.veterinaria.infrastructure.adapter.in.rest.dto.admin;

import java.time.LocalDate;

// Datos que se devuelven de una fila de la planilla de sanidad
public record PlanillaSanidadResponse(
        Long id,
        LocalDate fechaPlanilla,
        String diaSanidad,
        String areaLimpieza,
        String herramienta,
        Boolean limpiezaRealizada,
        Boolean desinfeccion) {
}
