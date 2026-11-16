
package usta.veterinaria.application.usecase.usuario;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import usta.veterinaria.application.port.in.usuario.AutenticarUsuarioUseCase;
import usta.veterinaria.application.port.out.usuario.PasswordVerifier;
import usta.veterinaria.application.port.out.usuario.UsuarioRepository;
import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.usuario.Usuario;

import java.util.Locale;

@ApplicationScoped
public class AutenticarUsuarioService
        implements AutenticarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordVerifier passwordVerifier;

    @Inject
    public AutenticarUsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordVerifier passwordVerifier) {

        this.usuarioRepository = usuarioRepository;
        this.passwordVerifier = passwordVerifier;
    }

    @Override
    public Usuario autenticar(String correo, String contrasena) {

        if (correo == null || correo.isBlank()) {
            throw new BusinessRuleException(
                    "El correo es obligatorio");
        }

        if (contrasena == null || contrasena.isBlank()) {
            throw new BusinessRuleException(
                    "La contraseña es obligatoria");
        }

        String correoNormalizado =
                correo.trim().toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository
                .findByCorreo(correoNormalizado)
                .orElseThrow(() ->
                        new BusinessRuleException(
                                "Credenciales incorrectas"));

        if (!passwordVerifier.verificar(
                contrasena,
                usuario.getContrasena())) {

            throw new BusinessRuleException(
                    "Credenciales incorrectas");
        }

        return usuario;
    }
}
