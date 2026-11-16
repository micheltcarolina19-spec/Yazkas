package usta.veterinaria.infrastructure.adapter.in.web;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import usta.veterinaria.application.port.in.usuario.AutenticarUsuarioUseCase;
import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.usuario.RolUsuario;
import usta.veterinaria.domain.model.usuario.Usuario;
import usta.veterinaria.infrastructure.adapter.in.web.sesion.SesionActual;
import usta.veterinaria.infrastructure.adapter.in.web.sesion.SesionCookie;

import java.net.URI;

@Path("/login")
public class LoginResource {

    private static final String RUTA_ADMIN = "/admin";
    private static final String RUTA_CLIENTE = "/mascotas";

    @Inject
    @Location("login.html")
    Template login;

    @Inject
    AutenticarUsuarioUseCase autenticarUsuarioUseCase;

    // Crea la cookie firmada de sesión (la misma que lee SesionActual)
    @Inject
    SesionCookie sesionCookie;

    @Inject
    SesionActual sesion;

    // Mostrar formulario de inicio de sesión (si ya hay sesión, va directo a su perfil)
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response mostrarLogin() {
        if (sesion.isActiva()) {
            return Response.seeOther(URI.create(sesion.getRutaPerfil())).build();
        }
        return Response.ok(vistaLogin(null, "")).build();
    }

    // Procesar inicio de sesión: guarda la cookie de sesión y redirige según el rol
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response iniciarSesion(@RestForm("correo") String correo,
                                  @RestForm("contrasena") String contrasena) {
        try {
            Usuario usuario = autenticarUsuarioUseCase.autenticar(correo, contrasena);
            String destino = rutaSegunRol(usuario.getRol());

            return Response.seeOther(URI.create(destino))
                    .cookie(sesionCookie.crear(usuario.getId(), usuario.getRol()))
                    .build();

        } catch (BusinessRuleException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(vistaLogin("Correo o contraseña incorrectos", correo))
                    .build();
        }
    }

    private String rutaSegunRol(RolUsuario rol) {
        if (rol == null) {
            throw new BusinessRuleException("El usuario no tiene un rol asignado");
        }
        return switch (rol) {
            case ADMIN -> RUTA_ADMIN;
            case CLIENTE -> RUTA_CLIENTE;
        };
    }

    private TemplateInstance vistaLogin(String error, String correo) {
        return login.instance()
                .data("error", error)
                .data("mensaje", null)
                .data("correo", correo == null ? "" : correo);
    }
}