package usta.veterinaria.infrastructure.adapter.in.rest.dto.admin;

import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import usta.veterinaria.domain.model.admin.AreaLimpieza;
import usta.veterinaria.domain.model.admin.Herramienta;

import java.time.LocalDate;

// Datos que llegan para registrar una fila de la planilla de limpieza y desinfección.
// Si no se envía la fecha se toma la de hoy; el día (LUNES..VIERNES) lo calcula el dominio
public record PlanillaSanidadRequest(
        @Schema(example = "2026-11-06")
        LocalDate fechaPlanilla,

        @Schema(example = "ZONA_LAVADO")
        @NotNull(message = "El área de limpieza es obligatorio") AreaLimpieza areaLimpieza,

        @Schema(example = "BANERA")
        @NotNull(message = "La herramienta o superficie es obligatoria") Herramienta herramienta,

        @Schema(example = "true")
        Boolean limpiezaRealizada,

        @Schema(example = "true")
        Boolean desinfeccion) {
}
