package usta.veterinaria.infrastructure.adapter.in.rest.dto.mascota;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import usta.veterinaria.domain.model.mascota.*;

import java.time.LocalDate;
import java.time.LocalTime;

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
    // Datos que se devuelven de la cita de una mascota (SIN precio).
    // duracionCita es ALTA, MEDIA o BAJA según lo que estimó la IA.
    // fotoMascota es la URL de la foto (puede ser null); nombrePropietario sirve para la agenda del dashboard
    public static record MascotaCitaResponse(
            Long id,
            Long agendamientoId,
            String estado,
            Long mascotaId,
            String nombreMascota,
            String razaMascota,
            String fotoMascota,
            String nombrePropietario,
            LocalDate fechaCreacion,
            LocalDate fechaAgendamiento,
            LocalTime horaAgendamiento,
            String duracionCita) {
    }

    // Datos que llegan para registrar o actualizar una mascota.
    // El id del propietario (o de la mascota al actualizar) va en la URL, no en el body.
    // Raza, pelaje y tamaño/peso se usan para que la IA estime la duración de las citas
    public static record MascotaRequest(
            @Schema(example = "Toby")
            @NotBlank(message = "El nombre de la mascota es obligatorio")
            @Size(max = 255, message = "El nombre no puede superar 255 caracteres") String nombre,

            @Schema(example = "2022-03-15")
            LocalDate fechaNacimiento,

            @Schema(example = "PERRO")
            @NotNull(message = "El tipo de mascota es obligatorio") TipoMascota tipo,

            @Schema(example = "Golden Retriever")
            @NotBlank(message = "La raza es obligatoria")
            @Size(max = 255, message = "La raza no puede superar 255 caracteres") String raza,

            @Schema(example = "MACHO")
            @NotNull(message = "El sexo es obligatorio") Sexo sexo,

            @Schema(example = "60")
            @Positive(message = "El tamaño debe ser mayor a 0") Float tamano,

            @Schema(example = "30.5")
            @Positive(message = "El peso debe ser mayor a 0") Float peso,

            @Schema(example = "Dorado")
            @Size(max = 255, message = "El color no puede superar 255 caracteres") String color,

            @Schema(example = "LARGO")
            @NotNull(message = "El pelaje es obligatorio") Pelaje pelaje,

            @Schema(example = "Le asusta el secador")
            @Size(max = 1000, message = "Las observaciones no puede superar 1000 caracteres") String observaciones,

            @Schema(example = "https://mi-servidor.com/fotos/toby.jpg")
            @Size(max = 255, message = "La URL de la foto no puede superar 255 caracteres") String fotoMascota,

            @Schema(example = "Juguetón")
            @Size(max = 255, message = "El temperamento no puede superar 255 caracteres") String temperamento,

            @Schema(example = "BAJA")
            NivelAgresividad nivelAgresividad,

            @Schema(example = "false")
            Boolean usaBozal,

            @Schema(example = "Ruidos fuertes")
            @Size(max = 1000, message = "Las fobias no puede superar 1000 caracteres") String fobias,

            @Schema(example = "true")
            Boolean sociable,

            @Schema(example = "ALTA")
            Energia energia) {
    }

    // Datos que se devuelven de una mascota (incluye el propietario)
    public static record MascotaResponse(
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
}