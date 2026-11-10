package usta.veterinaria.infrastructure.adapter.in.rest.dto.usuario;

// Datos que se devuelven de un usuario (nunca se devuelve la contraseña)
public record UsuarioResponse(
        Long id,
        String nombre,
        String apellido,
        String celular,
        String correo,
        String rol) {
}
