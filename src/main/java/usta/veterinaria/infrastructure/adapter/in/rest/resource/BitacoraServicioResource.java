package usta.veterinaria.infrastructure.adapter.in.rest.resource;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import usta.veterinaria.application.port.in.GestionarBitacoraServicioUseCase;
import usta.veterinaria.domain.model.BitacoraServicio;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.BitacoraServicioRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.BitacoraServicioResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.BitacoraServicioRestMapper;

// import jakarta.annotation.security.RolesAllowed;   // <- descomentar cuando exista seguridad (login)

import java.util.List;

// Adaptador de entrada REST para la bitácora: lo que se le hizo a la mascota en la cita y cuánto se cobró (COP).
// Lo usa el administrador al terminar la cita o al final del día.
@Path("/api/citas/{agendamientoId}/servicios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Bitacora de servicios", description = "Servicios prestados en cada cita y su valor")
public class BitacoraServicioResource {

    @Inject
    GestionarBitacoraServicioUseCase bitacora;

    // Registra un servicio prestado en la cita y responde 201
    @POST
    // @RolesAllowed("ADMIN")
    @Operation(summary = "Registrar un servicio prestado en una cita")
    public Response registrar(@PathParam("agendamientoId") Long agendamientoId,
                              @Valid @NotNull(message = "Los datos del servicio son obligatorios")
                              BitacoraServicioRequest request) {
        BitacoraServicio servicio = bitacora.registrar(
                agendamientoId, request.tipoServicio(), request.fotoSalida(), request.tipoBano(),
                request.precioSubtotal(), request.observacionesPeluqueria());
        return Response.status(Response.Status.CREATED)
                .entity(BitacoraServicioRestMapper.toResponse(servicio))
                .build();
    }

    // Lista los servicios registrados en la cita
    @GET
    // @RolesAllowed("ADMIN")
    @Operation(summary = "Listar los servicios de una cita")
    public List<BitacoraServicioResponse> listar(@PathParam("agendamientoId") Long agendamientoId) {
        return BitacoraServicioRestMapper.toResponseList(bitacora.listarPorAgendamiento(agendamientoId));
    }
}
