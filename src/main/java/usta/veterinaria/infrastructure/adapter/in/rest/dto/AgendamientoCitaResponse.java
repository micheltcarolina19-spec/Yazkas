package usta.veterinaria.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;

// Datos que se devuelven de un agendamiento (vista del administrador: incluye el precio total)
public record AgendamientoCitaResponse(
        Long id,
        String estado,
        BigDecimal precioTotal) {
}
