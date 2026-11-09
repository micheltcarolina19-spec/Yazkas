package usta.veterinaria.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

// Datos que llegan para actualizar los datos personales de un usuario.
// El id va en la URL; el rol y la contraseña no se cambian por aquí
public record ActualizarUsuarioRequest(
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
        @Size(max = 150, message = "El correo no puede superar 150 caracteres") String correo) {
}
