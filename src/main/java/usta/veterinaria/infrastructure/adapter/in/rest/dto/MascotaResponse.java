package usta.veterinaria.infrastructure.adapter.in.rest.dto;

import java.time.LocalDate;

// Datos que se devuelven de una mascota (incluye el propietario)
public record MascotaResponse(
        Long id,
        Long usuarioId,
        String nombrePropietario,
        String nombre,
        LocalDate fechaNacimiento,
        String tipo,
        String raza,
        String sexo,
        Float tamano,
        Float peso,
        String color,
        String pelaje,
        String observaciones,
        String fotoMascota,
        String temperamento,
        String nivelAgresividad,
        Boolean usaBozal,
        String fobias,
        Boolean sociable,
        String energia) {
}
