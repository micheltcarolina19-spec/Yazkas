package usta.veterinaria.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

// Datos del panel de finanzas en un rango de fechas. Todo en pesos colombianos (COP).
// Las fechas del rango llegan por query params (?inicio=...&fin=...), por eso no hay Request.
// Los mapas usan el nombre de la categoría como llave (ej: "ALQUILER_LOCAL" -> 800000)
public record ResumenFinancieroResponse(
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
