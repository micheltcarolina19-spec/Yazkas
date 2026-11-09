package usta.veterinaria.infrastructure.adapter.in.rest.dto;

// Edad de una mascota en años, meses y días (null si no tiene fecha de nacimiento)
public record EdadMascotaResponse(
        Long mascotaId,
        Integer anios,
        Integer meses,
        Integer dias) {
}
