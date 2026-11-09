package usta.veterinaria.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// Porcentaje de registros con limpieza y desinfección completas en un rango de fechas (ej: 80.00)
public record CumplimientoSanidadResponse(
        LocalDate inicio,
        LocalDate fin,
        BigDecimal porcentajeCumplimiento) {
}
