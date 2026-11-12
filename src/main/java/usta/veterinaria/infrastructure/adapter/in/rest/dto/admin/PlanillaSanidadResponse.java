package usta.veterinaria.infrastructure.adapter.in.rest.dto.admin;

import java.time.LocalDate;

// Datos que se devuelven de una fila de la planilla de sanidad.
// Los campos "...Etiqueta" son el texto legible para mostrar en pantalla (el otro es el valor del enum)
public record PlanillaSanidadResponse(
        Long id,
        LocalDate fechaPlanilla,
        String diaSanidad,
        String diaSanidadEtiqueta,
        String areaLimpieza,
        String areaLimpiezaEtiqueta,
        String herramienta,
        String herramientaEtiqueta,
        Boolean limpiezaRealizada,
        Boolean desinfeccion,
        String observaciones) {
}