package usta.veterinaria.infrastructure.adapter.in.rest.resource;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import usta.veterinaria.application.port.in.GestionarCitasUseCase;
import usta.veterinaria.domain.model.MascotaCita;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.AgendamientoCitaResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.MascotaCitaResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.ReprogramarCitaRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.AgendamientoCitaRestMapper;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.MascotaCitaRestMapper;

// import jakarta.annotation.security.RolesAllowed;   // <- descomentar cuando exista seguridad (login)

import java.time.LocalDate;
import java.util.List;

// Adaptador de entrada REST para gestionar las citas ya agendadas.
//   - Para AGENDAR se usa POST /api/usuarios/{usuarioId}/citas (UsuarioResource).
//   - Las respuestas del cliente usan MascotaCitaResponse (sin precio).
//   - Las del administrador usan AgendamientoCitaResponse (con el precio total).
@Path("/api/citas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Citas", description = "Reprogramar, cancelar, completar y consultar citas")
public class CitaResource {

    @Inject
    GestionarCitasUseCase citas;

    // Citas de todas las mascotas en un rango: /api/citas?inicio=2026-11-01&fin=2026-11-30
    @GET
    // @RolesAllowed("ADMIN")
    @Operation(summary = "Listar las citas en un rango de fechas")
    public List<MascotaCitaResponse> listarPorRango(@QueryParam("inicio")
                                                    @NotNull(message = "La fecha inicial es obligatoria") LocalDate inicio,
                                                    @QueryParam("fin")
                                                    @NotNull(message = "La fecha final es obligatoria") LocalDate fin) {
        return MascotaCitaRestMapper.toResponseList(citas.listarCitasPorRango(inicio, fin));
    }

    // Datos del agendamiento con su precio total (vista del administrador; 404 si no existe)
    @GET
    @Path("/{agendamientoId}")
    // @RolesAllowed("ADMIN")
    @Operation(summary = "Buscar un agendamiento por id (incluye el precio)")
    public AgendamientoCitaResponse buscarPorId(@PathParam("agendamientoId") Long agendamientoId) {
        return AgendamientoCitaRestMapper.toResponse(citas.buscarPorId(agendamientoId));
    }

    // Mascotas que hacen parte del agendamiento (vista del cliente: sin precio)
    @GET
    @Path("/{agendamientoId}/mascotas")
    @Operation(summary = "Listar las mascotas de un agendamiento")
    public List<MascotaCitaResponse> listarMascotas(@PathParam("agendamientoId") Long agendamientoId) {
        return MascotaCitaRestMapper.toResponseList(citas.listarCitasDelAgendamiento(agendamientoId));
    }

    // Cambia la fecha y hora de todas las mascotas del agendamiento
    @PUT
    @Path("/{agendamientoId}/reprogramar")
    @Operation(summary = "Reprogramar una cita")
    public List<MascotaCitaResponse> reprogramar(@PathParam("agendamientoId") Long agendamientoId,
                                                 @Valid @NotNull(message = "La nueva fecha y hora son obligatorias")
                                                 ReprogramarCitaRequest request) {
        List<MascotaCita> reprogramadas = citas.reprogramar(agendamientoId, request.nuevaFecha(), request.nuevaHora());
        return MascotaCitaRestMapper.toResponseList(reprogramadas);
    }

    // Cancela la cita. Responde con las mascotas del agendamiento (estado CANCELADA, sin precio)
    @PUT
    @Path("/{agendamientoId}/cancelar")
    @Operation(summary = "Cancelar una cita")
    public List<MascotaCitaResponse> cancelar(@PathParam("agendamientoId") Long agendamientoId) {
        citas.cancelar(agendamientoId);
        return MascotaCitaRestMapper.toResponseList(citas.listarCitasDelAgendamiento(agendamientoId));
    }

    // El administrador cierra la cita: el total sale de los servicios registrados en la bitácora
    // (POST /api/citas/{agendamientoId}/servicios) y pasa al panel de finanzas
    @PUT
    @Path("/{agendamientoId}/completar")
    // @RolesAllowed("ADMIN")
    @Operation(summary = "Completar una cita con lo cobrado en la bitácora")
    public AgendamientoCitaResponse completar(@PathParam("agendamientoId") Long agendamientoId) {
        return AgendamientoCitaRestMapper.toResponse(citas.completar(agendamientoId));
    }
}
