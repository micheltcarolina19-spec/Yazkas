package usta.veterinaria.infrastructure.adapter.in.rest.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

// Datos que llegan para sumar unidades al inventario. El id del producto va en la URL
public record ReabastecerProductoRequest(
        @Schema(example = "12")
        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a 0") Integer cantidad) {
}
