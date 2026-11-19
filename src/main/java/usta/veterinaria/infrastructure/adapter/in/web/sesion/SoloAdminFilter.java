package usta.veterinaria.infrastructure.adapter.in.web.sesion;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.net.URI;

// Deja pasar solo a quien tenga sesión iniciada con rol administrador
@Provider
@SoloAdmin
@Priority(Priorities.AUTHENTICATION)
public class SoloAdminFilter implements ContainerRequestFilter {

    @Inject
    SesionActual sesion;   // importa tu clase SesionActual (la del bean @Named("sesion"))

    @Override
    public void filter(ContainerRequestContext ctx) {
        if (!sesion.isActiva()) {
            ctx.abortWith(Response.seeOther(URI.create("/login")).build());
        } else if (!sesion.isAdmin()) {
            ctx.abortWith(Response.seeOther(URI.create("/")).build());
        }
    }
}