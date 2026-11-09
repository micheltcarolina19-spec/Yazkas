package usta.veterinaria.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

// Una línea de la venta: qué producto y cuántas unidades.
// No lleva precio: el subtotal se calcula con el precio de venta del producto
public record DetalleVentaRequest(
        @Schema(example = "1")
        @NotNull(message = "El producto es obligatorio") Long productoId,

        @Schema(example = "2")
        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a 0") Integer cantidad) {
}
