package usta.veterinaria.infrastructure.adapter.in.rest.dto.usuario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

// Datos que llegan para registrar un cliente junto con su primera mascota
// (un cliente siempre debe tener al menos una mascota)
public record RegistroClienteRequest(
        @Schema(example = "Ana")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres") String nombre,

        @Schema(example = "Pérez")
        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede superar 100 caracteres") String apellido,

        @Schema(example = "3001234567")
        @NotBlank(message = "El celular es obligatorio")
        @Size(max = 20, message = "El celular no puede superar 20 caracteres") String celular,

        @Schema(example = "ana.perez@correo.com")
        @NotBlank(message = "El correo es obligatorio")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres") String correo,

        @Schema(example = "ClaveSegura123")
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres") String contrasena,

        @Valid
        @NotNull(message = "La mascota es obligatoria") usta.veterinaria.infrastructure.adapter.in.rest.dto.mascota.DesparasitacionResponse.MascotaRequest mascota) {
}
