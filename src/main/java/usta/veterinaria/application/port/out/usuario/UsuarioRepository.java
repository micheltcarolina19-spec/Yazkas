package usta.veterinaria.application.port.out.usuario;

import usta.veterinaria.domain.model.usuario.Usuario;

import java.util.List;
import java.util.Optional;

// Puerto secundario: persistencia de usuarios
public interface UsuarioRepository {

    Usuario save(Usuario usuario);

    Optional<Usuario> findById(Long id);

    Optional<Usuario> findByCorreo(String correo);

    List<Usuario> findAll();
}
