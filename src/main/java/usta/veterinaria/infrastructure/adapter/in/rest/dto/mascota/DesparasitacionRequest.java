package usta.veterinaria.infrastructure.adapter.in.rest.dto.mascota;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import usta.veterinaria.domain.model.mascota.TipoDesparasitacion;

import java.time.LocalDate;

// Datos que llegan para registrar una desparasitación. El id de la mascota va en la URL
public record DesparasitacionRequest(
        @Schema(example = "NexGard")
        @NotBlank(message = "La marca es obligatoria")
        @Size(max = 255, message = "La marca no puede superar 255 caracteres") String marca,

        @Schema(example = "2026-11-01")
        @NotNull(message = "La fecha de aplicación es obligatoria") LocalDate fechaAplicacion,

        @Schema(example = "EXTERNA")
        @NotNull(message = "El tipo de desparasitación es obligatorio") TipoDesparasitacion tipo,

        @Schema(example = "NX-7781")
        @NotBlank(message = "El número de lote es obligatorio")
        @Size(max = 255, message = "El número de lote no puede superar 255 caracteres") String numeroLote,

        @Schema(example = "Próxima dosis en 3 meses")
        @Size(max = 1000, message = "Las observaciones no puede superar 1000 caracteres") String observaciones) {
}
