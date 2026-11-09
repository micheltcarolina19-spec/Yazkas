package usta.veterinaria.infrastructure.adapter.in.rest.dto;

import java.time.LocalDate;

// Datos que se devuelven de una vacuna aplicada
public record VacunacionResponse(
        Long id,
        Long mascotaId,
        String nombreMascota,
        String nombreVacuna,
        LocalDate fechaAplicacion,
        LocalDate fechaRefuerzo,
        String numeroLote,
        Integer numeroDosis,
        String observaciones) {
}
