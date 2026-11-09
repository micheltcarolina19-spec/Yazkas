package usta.veterinaria.infrastructure.adapter.in.rest.dto;

import java.time.LocalDateTime;

// Siguiente hora con cupo para la mascota a partir de la hora que pidió el cliente
public record EspacioDisponibleResponse(
        Long mascotaId,
        LocalDateTime horaDeseada,
        LocalDateTime siguienteEspacioDisponible) {
}
