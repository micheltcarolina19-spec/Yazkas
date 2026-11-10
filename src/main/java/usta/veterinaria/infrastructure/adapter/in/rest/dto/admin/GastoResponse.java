package usta.veterinaria.infrastructure.adapter.in.rest.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;

// Datos que se devuelven de un gasto
public record GastoResponse(
        Long id,
        String nombre,
        BigDecimal monto,
        LocalDate fechaGasto,
        String categoria) {
}
