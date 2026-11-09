package usta.veterinaria.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;

// Datos que llegan para registrar una vacuna aplicada. El id de la mascota va en la URL
public record VacunacionRequest(
        @Schema(example = "Rabia")
        @NotBlank(message = "El nombre de la vacuna es obligatorio")
        @Size(max = 150, message = "El nombre de la vacuna no puede superar 150 caracteres") String nombreVacuna,

        @Schema(example = "2026-11-01")
        @NotNull(message = "La fecha de aplicación es obligatoria") LocalDate fechaAplicacion,

        @Schema(example = "2027-11-01")
        LocalDate fechaRefuerzo,

        @Schema(example = "LT-48213")
        @Size(max = 100, message = "El número de lote no puede superar 100 caracteres") String numeroLote,

        @Schema(example = "1")
        @Min(value = 1, message = "El número de dosis debe ser 1 o mayor") Integer numeroDosis,

        @Schema(example = "Sin reacciones")
        @Size(max = 1000, message = "Las observaciones no puede superar 1000 caracteres") String observaciones) {
}
