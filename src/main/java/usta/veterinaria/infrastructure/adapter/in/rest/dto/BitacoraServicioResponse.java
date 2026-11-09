package usta.veterinaria.infrastructure.adapter.in.rest.dto;

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
}
