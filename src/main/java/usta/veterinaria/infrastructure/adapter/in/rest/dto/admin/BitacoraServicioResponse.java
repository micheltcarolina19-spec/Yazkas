package usta.veterinaria.infrastructure.adapter.in.rest.dto.admin;

import java.math.BigDecimal;

// Datos que se devuelven de un servicio registrado en la bitácora
public record BitacoraServicioResponse(
        Long id,
        Long agendamientoId,
        String tipoServicio,
        String fotoSalida,
        String tipoBano,
        BigDecimal precioSubtotal,
        String observacionesPeluqueria) {
    // Ganancia por unidad de un producto (precio de venta - precio base) en pesos colombianos
    public static record GananciaProductoResponse(
            Long productoId,
            BigDecimal gananciaUnitaria) {
    }
}
