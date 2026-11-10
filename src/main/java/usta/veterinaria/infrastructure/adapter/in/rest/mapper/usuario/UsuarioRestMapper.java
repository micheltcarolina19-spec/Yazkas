package usta.veterinaria.infrastructure.adapter.in.rest.mapper.usuario;

import usta.veterinaria.domain.model.usuario.Usuario;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.usuario.UsuarioResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.MapperUtils;

import java.util.List;

// Convierte un usuario del dominio a la respuesta REST.
// La contraseña NUNCA se copia a la respuesta.
public final class UsuarioRestMapper {

    private UsuarioRestMapper() {
    }

    public static UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCelular(),
                usuario.getCorreo(),
                MapperUtils.nombre(usuario.getRol()));
    }

    public static List<UsuarioResponse> toResponseList(List<Usuario> usuarios) {
        return MapperUtils.lista(usuarios, UsuarioRestMapper::toResponse);
    }
}
