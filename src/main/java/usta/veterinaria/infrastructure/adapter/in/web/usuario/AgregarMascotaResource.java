package usta.veterinaria.infrastructure.adapter.in.web.usuario;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import usta.veterinaria.domain.model.mascota.Energia;
import usta.veterinaria.domain.model.mascota.NivelAgresividad;
import usta.veterinaria.domain.model.mascota.Pelaje;
import usta.veterinaria.domain.model.mascota.Sexo;
import usta.veterinaria.domain.model.mascota.TipoMascota;
import usta.veterinaria.infrastructure.adapter.in.web.sesion.SesionActual;

import java.net.URI;

/**
 * Vista "Agregar Mascota" del cliente (mismos pasos y validaciones de la mascota en /registro).
 * Plantilla: src/main/resources/templates/usuarios/agregarMascota.html
 * El dueño sale de la sesión; el formulario guarda con POST /api/usuarios/{usuarioId}/mascotas
 * y, si se llenó, la historia con POST /api/mascotas/{mascotaId}/historia-clinica.
 */
@Path("/mascotas/agregar")
public class AgregarMascotaResource {

    @Inject
    @Location("usuarios/agregarMascota.html")
    Template agregarMascota;

    @Inject
    SesionActual sesion;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response ver() {
        if (!sesion.isActiva()) {
            return redirigir("/login");
        }
        if (!sesion.isCliente()) {
            return redirigir(sesion.getRutaPerfil());
        }
        // Los mismos enums que usa RegistroResource para los selectores de la mascota
        return Response.ok(agregarMascota
                .data("usuarioId", sesion.getUsuarioId())
                .data("tipos", TipoMascota.values())
                .data("sexos", Sexo.values())
                .data("pelajes", Pelaje.values())
                .data("energias", Energia.values())
                .data("agresividades", NivelAgresividad.values())).build();
    }

    private Response redirigir(String ruta) {
        return Response.seeOther(URI.create(ruta)).build();
    }
}
