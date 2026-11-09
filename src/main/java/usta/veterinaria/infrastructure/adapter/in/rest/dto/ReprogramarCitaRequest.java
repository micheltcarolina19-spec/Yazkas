package usta.veterinaria.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

// Datos que llegan para reprogramar una cita. El id del agendamiento va en la URL
public record ReprogramarCitaRequest(
        @Schema(example = "2026-11-21")
        @NotNull(message = "La nueva fecha es obligatoria") LocalDate nuevaFecha,

        @Schema(example = "10:30:00")
        @NotNull(message = "La nueva hora es obligatoria") LocalTime nuevaHora) {
}
