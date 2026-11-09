package usta.veterinaria.infrastructure.adapter.in.rest.dto;

import java.time.LocalDate;

// Datos que se devuelven de una desparasitación
public record DesparasitacionResponse(
        Long id,
        Long mascotaId,
        String nombreMascota,
        String marca,
        LocalDate fechaAplicacion,
        String tipo,
        String numeroLote,
        String observaciones) {
}
