package usta.veterinaria.infrastructure.adapter.in.rest.dto.admin;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import usta.veterinaria.domain.model.admin.CategoriaProducto;
import usta.veterinaria.domain.model.admin.TipoProducto;

import java.math.BigDecimal;

// Datos que llegan para crear o actualizar un producto (solo el administrador).
// Los precios van en pesos colombianos. Si no se envía el stock se toma 0
public record ProductoRequest(
        @Schema(example = "Dog Chow Adultos 4 kg")
        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(max = 255, message = "El nombre no puede superar 255 caracteres") String nombre,

        @Schema(example = "Purina")
        @Size(max = 255, message = "La marca no puede superar 255 caracteres") String marca,

        @Schema(example = "CONCENTRADOS")
        @NotNull(message = "La categoría es obligatoria") CategoriaProducto categoria,

        @Schema(example = "Alimento para perros adultos de razas medianas")
        @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres") String descripcion,

        @Schema(example = "PREMIUM")
        @NotNull(message = "El tipo de producto es obligatorio") TipoProducto tipo,

        @Schema(example = "40000")
        @NotNull(message = "El precio base es obligatorio")
        @Positive(message = "El precio base debe ser mayor a 0")
        @Digits(integer = 8, fraction = 2, message = "El precio base no es un valor válido en pesos") BigDecimal precioBase,

        @Schema(example = "52000")
        @NotNull(message = "El precio de venta es obligatorio")
        @Positive(message = "El precio de venta debe ser mayor a 0")
        @Digits(integer = 8, fraction = 2, message = "El precio de venta no es un valor válido en pesos") BigDecimal precioVenta,

        @Schema(example = "https://mi-servidor.com/fotos/dog-chow.jpg")
        @Size(max = 255, message = "La URL de la foto no puede superar 255 caracteres") String fotoProducto,

        @Schema(example = "10")
        @PositiveOrZero(message = "El stock no puede ser negativo") Integer stock) {
}
