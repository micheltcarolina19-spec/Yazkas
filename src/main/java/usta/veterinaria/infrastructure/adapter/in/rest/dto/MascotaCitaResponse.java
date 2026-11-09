package usta.veterinaria.infrastructure.adapter.in.rest.dto;

import java.time.LocalDate;
import java.time.LocalTime;

// Datos que se devuelven de la cita de una mascota (vista del cliente: SIN precio).
// duracionCita es ALTA, MEDIA o BAJA según lo que estimó la IA
public record MascotaCitaResponse(
        Long id,
        Long agendamientoId,
        String estado,
        Long mascotaId,
        String nombreMascota,
        LocalDate fechaCreacion,
        LocalDate fechaAgendamiento,
        LocalTime horaAgendamiento,
        String duracionCita) {
}
