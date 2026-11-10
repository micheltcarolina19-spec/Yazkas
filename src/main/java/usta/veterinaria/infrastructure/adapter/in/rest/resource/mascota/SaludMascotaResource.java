package usta.veterinaria.infrastructure.adapter.in.rest.resource.mascota;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import usta.veterinaria.application.port.in.mascota.GestionarSaludMascotaUseCase;
import usta.veterinaria.domain.model.mascota.Desparasitacion;
import usta.veterinaria.domain.model.mascota.Vacunacion;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.mascota.DesparasitacionRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.mascota.DesparasitacionResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.mascota.VacunacionRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.mascota.VacunacionResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.admin.ProductoRestMapper;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.mascota.VacunacionRestMapper;

// import jakarta.annotation.security.RolesAllowed;   // <- descomentar cuando exista seguridad (login)

import java.util.List;

// Adaptador de entrada REST para el carnet de la mascota: vacunas y desparasitaciones
@Path("/api/salud")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Salud", description = "Vacunas, desparasitaciones y recordatorios de refuerzo")
public class SaludMascotaResource {

    @Inject
    GestionarSaludMascotaUseCase salud;

    // Registra una vacuna aplicada a la mascota y responde 201
    @POST
    @Path("/mascotas/{mascotaId}/vacunaciones")
    @Operation(summary = "Registrar una vacuna de una mascota")
    public Response registrarVacunacion(@PathParam("mascotaId") Long mascotaId,
                                        @Valid @NotNull(message = "Los datos de la vacuna son obligatorios")
                                        VacunacionRequest request) {
        Vacunacion vacunacion = salud.registrarVacunacion(
                mascotaId, request.nombreVacuna(), request.fechaAplicacion(), request.fechaRefuerzo(),
                request.numeroLote(), request.numeroDosis(), request.observaciones());
        return Response.status(Response.Status.CREATED)
                .entity(VacunacionRestMapper.toResponse(vacunacion))
                .build();
    }

    // Lista las vacunas de la mascota
    @GET
    @Path("/mascotas/{mascotaId}/vacunaciones")
    @Operation(summary = "Listar las vacunas de una mascota")
    public List<VacunacionResponse> listarVacunaciones(@PathParam("mascotaId") Long mascotaId) {
        return VacunacionRestMapper.toResponseList(salud.listarVacunaciones(mascotaId));
    }

    // Registra una desparasitación de la mascota y responde 201
    @POST
    @Path("/mascotas/{mascotaId}/desparasitaciones")
    @Operation(summary = "Registrar una desparasitación de una mascota")
    public Response registrarDesparasitacion(@PathParam("mascotaId") Long mascotaId,
                                             @Valid @NotNull(message = "Los datos de la desparasitación son obligatorios")
                                             DesparasitacionRequest request) {
        Desparasitacion desparasitacion = salud.registrarDesparasitacion(
                mascotaId, request.marca(), request.fechaAplicacion(), request.tipo(),
                request.numeroLote(), request.observaciones());
        return Response.status(Response.Status.CREATED)
                .entity(ProductoRestMapper.DesparasitacionRestMapper.toResponse(desparasitacion))
                .build();
    }

    // Lista las desparasitaciones de la mascota
    @GET
    @Path("/mascotas/{mascotaId}/desparasitaciones")
    @Operation(summary = "Listar las desparasitaciones de una mascota")
    public List<DesparasitacionResponse> listarDesparasitaciones(@PathParam("mascotaId") Long mascotaId) {
        return ProductoRestMapper.DesparasitacionRestMapper.toResponseList(salud.listarDesparasitaciones(mascotaId));
    }

    // Vacunas cuyo refuerzo toca pronto: /api/salud/vacunaciones/refuerzos-proximos?dias=15 (por defecto 30)
    @GET
    @Path("/vacunaciones/refuerzos-proximos")
    // @RolesAllowed("ADMIN")
    @Operation(summary = "Listar las vacunas con refuerzo en los próximos días")
    public List<VacunacionResponse> refuerzosProximos(@QueryParam("dias") @DefaultValue("30")
                                                      @PositiveOrZero(message = "Los días deben ser 0 o más")
                                                      int dias) {
        return VacunacionRestMapper.toResponseList(salud.refuerzosProximos(dias));
    }

    // Vacunas cuyo refuerzo ya se venció
    @GET
    @Path("/vacunaciones/refuerzos-vencidos")
    // @RolesAllowed("ADMIN")
    @Operation(summary = "Listar las vacunas con refuerzo vencido")
    public List<VacunacionResponse> refuerzosVencidos() {
        return VacunacionRestMapper.toResponseList(salud.refuerzosVencidos());
    }
}
