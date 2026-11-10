package usta.veterinaria.infrastructure.adapter.in.rest.dto.usuario;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// Datos que llegan para agendar una cita de una o varias mascotas del mismo usuario.
// El id del usuario va en la URL. No lleva precio ni duración: la duración la estima la IA
public record AgendarCitaRequest(
        @Schema(example = "[1, 2]")
        @NotEmpty(message = "Debe seleccionar al menos una mascota") List<@NotNull Long> mascotaIds,

        @Schema(example = "2026-11-20")
        @NotNull(message = "La fecha de la cita es obligatoria") LocalDate fechaAgendamiento,

        @Schema(example = "09:00:00")
        @NotNull(message = "La hora de la cita es obligatoria") LocalTime horaAgendamiento) {
}
