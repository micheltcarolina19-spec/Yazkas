package usta.veterinaria.infrastructure.adapter.in.rest.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

// Una línea de la venta: qué producto y cuántas unidades.
// No lleva precio: el subtotal se calcula con el precio de venta del producto
public record DetalleVentaRequest(
        @Schema(example = "1")
        @NotNull(message = "El producto es obligatorio") Long productoId,

        @Schema(example = "2")
        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a 0") Integer cantidad) {
    // Datos del panel de finanzas en un rango de fechas. Todo en pesos colombianos (COP).
    // Las fechas del rango llegan por query params (?inicio=...&fin=...), por eso no hay Request.
    // Los mapas usan el nombre de la categoría como llave (ej: "ALQUILER_LOCAL" -> 800000)
    public static record ResumenFinancieroResponse(
            LocalDate inicio,
            LocalDate fin,
            BigDecimal ingresosPorVentas,
            BigDecimal ingresosPorCitas,
            BigDecimal ingresosTotales,
            BigDecimal totalGastos,
            BigDecimal utilidad,
            BigDecimal margenUtilidad,
            BigDecimal gananciaProductos,
            Map<String, BigDecimal> gastosPorCategoria,
            Map<String, BigDecimal> ventasPorCategoriaProducto) {
    }
}
