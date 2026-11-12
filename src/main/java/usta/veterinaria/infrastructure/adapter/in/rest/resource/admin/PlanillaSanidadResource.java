package usta.veterinaria.infrastructure.adapter.in.rest.resource.admin;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import usta.veterinaria.application.port.in.admin.GestionarPlanillaSanidadUseCase;
import usta.veterinaria.domain.model.admin.AreaLimpieza;
import usta.veterinaria.domain.model.admin.DiaSanidad;
import usta.veterinaria.domain.model.admin.Herramienta;
import usta.veterinaria.domain.model.admin.PlanillaSanidad;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.admin.PlanillaSanidadRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.admin.PlanillaSanidadResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.admin.PlanillaSanidadRestMapper;
import usta.veterinaria.infrastructure.adapter.in.rest.resource.admin.PlanillaSanidadPdf;
// import jakarta.annotation.security.RolesAllowed;   // <- descomentar cuando exista seguridad (login)

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

// Adaptador de entrada REST de la planilla de limpieza y desinfección (CRUD + PDF)
@Path("/api/planilla-sanidad")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Planilla de sanidad", description = "Registro de limpieza y desinfección de lunes a viernes")
// @RolesAllowed("ADMIN")   // <- protege TODOS los endpoints de esta clase
public class PlanillaSanidadResource {

    @Inject
    GestionarPlanillaSanidadUseCase planilla;

    // Datos que salen en el encabezado del PDF (se cambian en application.properties)
    @ConfigProperty(name = "yazkas.sanidad.responsable", defaultValue = "Administrador")
    String responsablePorDefecto;

    @ConfigProperty(name = "yazkas.sanidad.sede", defaultValue = "Sede Principal")
    String sede;

    // Listado: /api/planilla-sanidad?inicio=2026-11-01&fin=2026-11-30
    @GET
    @Operation(summary = "Listar los registros de la planilla en un rango de fechas")
    public List<PlanillaSanidadResponse> listar(
            @QueryParam("inicio") @NotNull(message = "La fecha inicial es obligatoria") LocalDate inicio,
            @QueryParam("fin") @NotNull(message = "La fecha final es obligatoria") LocalDate fin) {
        return PlanillaSanidadRestMapper.toResponse(planilla.listarPorRango(inicio, fin));
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consultar un registro de la planilla")
    public PlanillaSanidadResponse buscar(@PathParam("id") Long id) {
        return PlanillaSanidadRestMapper.toResponse(planilla.buscarPorId(id));
    }

    @POST
    @Operation(summary = "Registrar una fila de la planilla")
    public Response registrar(@Valid @NotNull(message = "El cuerpo de la petición es obligatorio") PlanillaSanidadRequest request) {
        PlanillaSanidad creada = planilla.registrar(
                request.fechaPlanilla(),
                request.areaLimpieza(),
                request.herramienta(),
                request.limpiezaRealizada(),
                request.desinfeccion(),
                request.observaciones());
        return Response.status(Response.Status.CREATED)
                .entity(PlanillaSanidadRestMapper.toResponse(creada))
                .build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Editar una fila de la planilla")
    public PlanillaSanidadResponse actualizar(
            @PathParam("id") Long id,
            @Valid @NotNull(message = "El cuerpo de la petición es obligatorio") PlanillaSanidadRequest request) {
        PlanillaSanidad actualizada = planilla.actualizar(
                id,
                request.fechaPlanilla(),
                request.areaLimpieza(),
                request.herramienta(),
                request.limpiezaRealizada(),
                request.desinfeccion(),
                request.observaciones());
        return PlanillaSanidadRestMapper.toResponse(actualizada);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Eliminar una fila de la planilla")
    public Response eliminar(@PathParam("id") Long id) {
        planilla.eliminar(id);
        return Response.noContent().build();
    }

    // Porcentaje de filas con limpieza y desinfección completas (80.00 = 80 %)
    @GET
    @Path("/cumplimiento")
    @Operation(summary = "Consultar el porcentaje de cumplimiento de un rango de fechas")
    public BigDecimal cumplimiento(
            @QueryParam("inicio") @NotNull(message = "La fecha inicial es obligatoria") LocalDate inicio,
            @QueryParam("fin") @NotNull(message = "La fecha final es obligatoria") LocalDate fin) {
        return planilla.porcentajeCumplimiento(inicio, fin);
    }

    // Opciones para llenar los select del formulario (valor del enum + texto legible)
    @GET
    @Path("/opciones")
    @Operation(summary = "Consultar las áreas, herramientas y días disponibles")
    public Opciones opciones() {
        return new Opciones(
                Arrays.stream(AreaLimpieza.values()).map(PlanillaSanidadResource::opcion).toList(),
                Arrays.stream(Herramienta.values()).map(PlanillaSanidadResource::opcion).toList(),
                Arrays.stream(DiaSanidad.values()).map(PlanillaSanidadResource::opcion).toList());
    }

    // PDF firmado: /api/planilla-sanidad/pdf?inicio=2026-11-01&fin=2026-11-30
    @GET
    @Path("/pdf")
    @Produces("application/pdf")
    @Operation(summary = "Descargar la planilla de sanidad en PDF para un rango de fechas")
    public Response pdf(
            @QueryParam("inicio") @NotNull(message = "La fecha inicial es obligatoria") LocalDate inicio,
            @QueryParam("fin") @NotNull(message = "La fecha final es obligatoria") LocalDate fin,
            @QueryParam("responsable") String responsable) {
        List<PlanillaSanidad> registros = planilla.listarPorRango(inicio, fin);
        BigDecimal cumplimiento = planilla.porcentajeCumplimiento(inicio, fin);
        String firmante = responsable == null || responsable.isBlank() ? responsablePorDefecto : responsable.trim();

        byte[] documento = PlanillaSanidadPdf.generar(registros, inicio, fin, cumplimiento, sede, firmante);
        String nombre = "planilla-sanidad_" + inicio + "_" + fin + ".pdf";
        return Response.ok(documento)
                .header("Content-Disposition", "attachment; filename=\"" + nombre + "\"")
                .build();
    }

    private static Opcion opcion(Enum<?> valor) {
        return new Opcion(valor.name(), PlanillaSanidadRestMapper.etiqueta(valor));
    }

    public record Opcion(String valor, String etiqueta) {
    }

    public record Opciones(List<Opcion> areas, List<Opcion> herramientas, List<Opcion> dias) {
    }
}