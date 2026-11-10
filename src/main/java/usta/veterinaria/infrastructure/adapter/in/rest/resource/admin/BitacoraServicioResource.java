package usta.veterinaria.infrastructure.adapter.in.rest.resource.admin;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import usta.veterinaria.application.port.in.admin.GestionarBitacoraServicioUseCase;
import usta.veterinaria.application.port.in.admin.GestionarPlanillaSanidadUseCase;
import usta.veterinaria.domain.model.admin.BitacoraServicio;
import usta.veterinaria.domain.model.admin.PlanillaSanidad;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.admin.*;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.admin.FinanzasRestMapper;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.admin.PlanillaSanidadRestMapper;

// import jakarta.annotation.security.RolesAllowed;   // <- descomentar cuando exista seguridad (login)

import java.time.LocalDate;
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
                .entity(FinanzasRestMapper.BitacoraServicioRestMapper.toResponse(servicio))
                .build();
    }

    // Lista los servicios registrados en la cita
    @GET
    // @RolesAllowed("ADMIN")
    @Operation(summary = "Listar los servicios de una cita")
    public List<BitacoraServicioResponse> listar(@PathParam("agendamientoId") Long agendamientoId) {
        return FinanzasRestMapper.BitacoraServicioRestMapper.toResponseList(bitacora.listarPorAgendamiento(agendamientoId));
    }

    // Adaptador de entrada REST para la planilla de limpieza y desinfección (lunes a viernes)
    @Path("/api/sanidad")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = "Sanidad", description = "Planilla de limpieza y desinfección")
    // @RolesAllowed("ADMIN")   // <- protege TODOS los endpoints de esta clase
    public static class PlanillaSanidadResource {

        @Inject
        GestionarPlanillaSanidadUseCase planillas;

        // Registra una fila de la planilla y responde 201
        @POST
        @Operation(summary = "Registrar una limpieza en la planilla de sanidad")
        public Response registrar(@Valid @NotNull(message = "Los datos de la planilla son obligatorios")
                                  PlanillaSanidadRequest request) {
            PlanillaSanidad planilla = planillas.registrar(
                    request.fechaPlanilla(), request.areaLimpieza(), request.herramienta(),
                    request.limpiezaRealizada(), request.desinfeccion());
            return Response.status(Response.Status.CREATED)
                    .entity(PlanillaSanidadRestMapper.toResponse(planilla))
                    .build();
        }

        // Registros de un día: /api/sanidad/dia?fecha=2026-11-06
        @GET
        @Path("/dia")
        @Operation(summary = "Listar la planilla de un día")
        public List<PlanillaSanidadResponse> listarPorFecha(@QueryParam("fecha")
                                                            @NotNull(message = "La fecha es obligatoria") LocalDate fecha) {
            return PlanillaSanidadRestMapper.toResponseList(planillas.listarPorFecha(fecha));
        }

        // Registros en un rango: /api/sanidad?inicio=2026-11-01&fin=2026-11-30
        @GET
        @Operation(summary = "Listar la planilla en un rango de fechas")
        public List<PlanillaSanidadResponse> listarPorRango(@QueryParam("inicio")
                                                            @NotNull(message = "La fecha inicial es obligatoria") LocalDate inicio,
                                                            @QueryParam("fin")
                                                            @NotNull(message = "La fecha final es obligatoria") LocalDate fin) {
            return PlanillaSanidadRestMapper.toResponseList(planillas.listarPorRango(inicio, fin));
        }

        // Porcentaje de cumplimiento: /api/sanidad/cumplimiento?inicio=2026-11-01&fin=2026-11-30
        @GET
        @Path("/cumplimiento")
        @Operation(summary = "Consultar el porcentaje de cumplimiento de limpieza y desinfección")
        public CumplimientoSanidadResponse cumplimiento(@QueryParam("inicio")
                                                        @NotNull(message = "La fecha inicial es obligatoria") LocalDate inicio,
                                                        @QueryParam("fin")
                                                        @NotNull(message = "La fecha final es obligatoria") LocalDate fin) {
            return PlanillaSanidadRestMapper.toCumplimientoResponse(
                    inicio, fin, planillas.porcentajeCumplimiento(inicio, fin));
        }
    }
}
