package usta.veterinaria.infrastructure.adapter.in.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import usta.veterinaria.infrastructure.adapter.in.web.sesion.SesionCookie;

import java.net.URI;

@Path("/logout")
public class LogoutResource {

    @Inject
    SesionCookie sesionCookie;

    // Borra la cookie de sesión y vuelve al inicio
    @GET
    public Response cerrarSesion() {
        return Response.seeOther(URI.create("/"))
                .cookie(sesionCookie.cerrar())
                .build();
    }
}
