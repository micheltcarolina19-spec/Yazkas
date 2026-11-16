package usta.veterinaria.infrastructure.adapter.in.web.sesion;

import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import usta.veterinaria.domain.model.usuario.RolUsuario;
import usta.veterinaria.infrastructure.adapter.in.web.sesion.SesionCookie.DatosSesion;

import java.util.Optional;

/**
 * Sesión del usuario en la petición actual.
 * - En los resources: @Inject SesionActual sesion;
 * - En las plantillas Qute: {inject:sesion.activa}, {inject:sesion.rutaPerfil}, {inject:sesion.admin}
 */
@Named("sesion")
@RequestScoped
public class SesionActual {

    @Inject
    HttpServerRequest request;

    @Inject
    SesionCookie sesionCookie;

    private Optional<DatosSesion> datos;

    private Optional<DatosSesion> datos() {
        if (datos == null) {
            Cookie cookie = request.getCookie(SesionCookie.NOMBRE);
            datos = sesionCookie.leer(cookie == null ? null : cookie.getValue());
        }
        return datos;
    }

    public boolean isActiva() {
        return datos().isPresent();
    }

    public boolean isAdmin() {
        return datos().map(d -> d.rol() == RolUsuario.ADMIN).orElse(false);
    }

    public boolean isCliente() {
        return datos().map(d -> d.rol() == RolUsuario.CLIENTE).orElse(false);
    }

    public Long getUsuarioId() {
        return datos().map(DatosSesion::usuarioId).orElse(null);
    }

    // A dónde lleva el botón de perfil del navbar
    public String getRutaPerfil() {
        if (!isActiva()) {
            return "/login";
        }
        return isAdmin() ? "/admin" : "/mascotas";
    }
}
