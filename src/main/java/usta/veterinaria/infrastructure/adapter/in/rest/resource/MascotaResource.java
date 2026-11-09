package usta.veterinaria.infrastructure.adapter.in.rest.resource;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import usta.veterinaria.application.port.in.GestionarCitasUseCase;
import usta.veterinaria.application.port.in.GestionarMascotasUseCase;
import usta.veterinaria.domain.model.Mascota;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.EdadMascotaResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.EspacioDisponibleResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.MascotaCitaResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.MascotaRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.MascotaResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.MascotaCitaRestMapper;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.MascotaRestMapper;

// import jakarta.annotation.security.RolesAllowed;   // <- descomentar cuando exista seguridad (login)

import java.time.LocalDateTime;
import java.util.List;

// Adaptador de entrada REST para las mascotas.
// Para REGISTRAR una mascota se usa POST /api/usuarios/{usuarioId}/mascotas (UsuarioResource).
@Path("/api/mascotas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Mascotas", description = "Consulta y actualización de mascotas, su edad y sus citas")
public class MascotaResource {

    @Inject
    GestionarMascotasUseCase mascotas;

    @Inject
    GestionarCitasUseCase citas;

    // Lista todas las mascotas
    @GET
    // @RolesAllowed("ADMIN")
    @Operation(summary = "Listar todas las mascotas")
    public List<MascotaResponse> listar() {
        return MascotaRestMapper.toResponseList(mascotas.listar());
    }

    // Busca una mascota por id (404 si no existe)
    @GET
    @Path("/{id}")
    @Operation(summary = "Buscar una mascota por id")
    public MascotaResponse buscarPorId(@PathParam("id") Long id) {
        return MascotaRestMapper.toResponse(mascotas.buscarPorId(id));
    }

    // Actualiza los datos de una mascota (404 si no existe)
    @PUT
    @Path("/{id}")
    @Operation(summary = "Actualizar una mascota")
    public MascotaResponse actualizar(@PathParam("id") Long id,
                                      @Valid @NotNull(message = "Los datos de la mascota son obligatorios")
                                      MascotaRequest request) {
        Mascota mascota = mascotas.actualizar(
                id, request.nombre(), request.fechaNacimiento(), request.tipo(), request.raza(),
                request.sexo(), request.tamano(), request.peso(), request.color(), request.pelaje(),
                request.observaciones(), request.fotoMascota(), request.temperamento(),
                request.nivelAgresividad(), request.usaBozal(), request.fobias(), request.sociable(),
                request.energia());
        return MascotaRestMapper.toResponse(mascota);
    }

    // Elimina una mascota y responde 204 (409 si tiene información relacionada)
    @DELETE
    @Path("/{id}")
    @Operation(summary = "Eliminar una mascota")
    public Response eliminar(@PathParam("id") Long id) {
        mascotas.eliminar(id);
        return Response.noContent().build();
    }

    // Edad de la mascota en años, meses y días
    @GET
    @Path("/{id}/edad")
    @Operation(summary = "Calcular la edad de una mascota")
    public EdadMascotaResponse edad(@PathParam("id") Long id) {
        return MascotaRestMapper.toEdadResponse(id, mascotas.calcularEdad(id));
    }

    // Historial de citas de la mascota (vista del cliente: sin precio)
    @GET
    @Path("/{id}/citas")
    @Operation(summary = "Listar las citas de una mascota")
    public List<MascotaCitaResponse> listarCitas(@PathParam("id") Long id) {
        return MascotaCitaRestMapper.toResponseList(citas.listarCitasPorMascota(id));
    }

    // Siguiente hora con cupo para la mascota:
    // /api/mascotas/5/siguiente-espacio?horaDeseada=2026-11-20T09:00:00
    @GET
    @Path("/{id}/siguiente-espacio")
    @Operation(summary = "Consultar el siguiente espacio disponible para agendar a la mascota")
    public EspacioDisponibleResponse siguienteEspacio(@PathParam("id") Long id,
                                                      @QueryParam("horaDeseada")
                                                      @NotNull(message = "La hora deseada es obligatoria")
                                                      LocalDateTime horaDeseada) {
        LocalDateTime siguiente = citas.siguienteEspacioDisponible(id, horaDeseada);
        return MascotaCitaRestMapper.toEspacioDisponibleResponse(id, horaDeseada, siguiente);
    }
}
