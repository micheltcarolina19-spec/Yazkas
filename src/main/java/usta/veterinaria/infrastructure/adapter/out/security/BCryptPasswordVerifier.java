
package usta.veterinaria.infrastructure.adapter.out.security;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import usta.veterinaria.application.port.out.usuario.PasswordVerifier;

@ApplicationScoped
public class BCryptPasswordVerifier implements PasswordVerifier {

    @Override
    public boolean verificar(String contrasenaIngresada,
                             String contrasenaCifrada) {

        if (contrasenaIngresada == null || contrasenaCifrada == null) {
            return false;
        }

        return BcryptUtil.matches(
                contrasenaIngresada,
                contrasenaCifrada
        );
    }
}
