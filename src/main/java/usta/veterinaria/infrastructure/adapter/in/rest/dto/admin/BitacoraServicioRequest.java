package usta.veterinaria.infrastructure.adapter.in.rest.dto.admin;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import usta.veterinaria.domain.model.admin.TipoBano;

import java.math.BigDecimal;

// Datos que llegan para registrar un servicio prestado en una cita y lo que se cobró (COP).
// El id del agendamiento va en la URL
public record BitacoraServicioRequest(
        @Schema(example = "Baño y corte")
        @NotBlank(message = "El tipo de servicio es obligatorio")
        @Size(max = 255, message = "El tipo de servicio no puede superar 255 caracteres") String tipoServicio,

        @Schema(example = "https://mi-servidor.com/fotos/toby-salida.jpg")
        @Size(max = 255, message = "La URL de la foto no puede superar 255 caracteres") String fotoSalida,

        @Schema(example = "NORMAL")
        TipoBano tipoBano,

        @Schema(example = "45000")
        @NotNull(message = "El precio del servicio es obligatorio")
        @Positive(message = "El precio del servicio debe ser mayor a 0")
        @Digits(integer = 8, fraction = 2, message = "El precio del servicio no es un valor válido en pesos") BigDecimal precioSubtotal,

        @Schema(example = "Tenía nudos en las orejas")
        @Size(max = 1000, message = "Las observaciones no puede superar 1000 caracteres") String observacionesPeluqueria) {
}
