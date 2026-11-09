package usta.veterinaria.infrastructure.adapter.in.rest.resource;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import usta.veterinaria.application.port.in.GestionarPlanillaSanidadUseCase;
import usta.veterinaria.domain.model.PlanillaSanidad;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.CumplimientoSanidadResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.PlanillaSanidadRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.PlanillaSanidadResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.PlanillaSanidadRestMapper;

// import jakarta.annotation.security.RolesAllowed;   // <- descomentar cuando exista seguridad (login)

import java.time.LocalDate;
import java.util.List;

// Adaptador de entrada REST para la planilla de limpieza y desinfección (lunes a viernes)
@Path("/api/sanidad")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Sanidad", description = "Planilla de limpieza y desinfección")
// @RolesAllowed("ADMIN")   // <- protege TODOS los endpoints de esta clase
public class PlanillaSanidadResource {

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
