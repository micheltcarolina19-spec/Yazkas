
package usta.veterinaria.infrastructure.adapter.out.security;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import usta.veterinaria.application.port.out.usuario.PasswordHasher;

@ApplicationScoped
public class BCryptPasswordHasher implements PasswordHasher {

    @Override
    public String cifrar(String contrasena) {
        return BcryptUtil.bcryptHash(contrasena);
    }
}
