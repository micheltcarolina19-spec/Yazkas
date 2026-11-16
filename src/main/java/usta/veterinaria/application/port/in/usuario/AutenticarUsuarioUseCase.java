
package usta.veterinaria.application.port.in.usuario;

import usta.veterinaria.domain.model.usuario.Usuario;

public interface AutenticarUsuarioUseCase {

    Usuario autenticar(String correo, String contrasena);

}
