package usta.veterinaria.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import usta.veterinaria.domain.model.CategoriaGasto;

import java.math.BigDecimal;
import java.time.LocalDate;

// Datos que llegan para registrar o actualizar un gasto (en pesos colombianos).
// Si no se envía la fecha se toma la de hoy
public record GastoRequest(
        @Schema(example = "Arriendo del local")
        @NotBlank(message = "El nombre del gasto es obligatorio")
        @Size(max = 255, message = "El nombre no puede superar 255 caracteres") String nombre,

        @Schema(example = "800000")
        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a 0")
        @Digits(integer = 8, fraction = 2, message = "El monto no es un valor válido en pesos") BigDecimal monto,

        @Schema(example = "2026-11-01")
        LocalDate fechaGasto,

        @Schema(example = "ALQUILER_LOCAL")
        @NotNull(message = "La categoría es obligatoria") CategoriaGasto categoria) {
}
