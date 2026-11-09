package usta.veterinaria.infrastructure.adapter.in.rest.resource;

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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import usta.veterinaria.application.port.in.GestionarGastosUseCase;
import usta.veterinaria.domain.model.Gasto;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.GastoRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.GastoResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.GastoRestMapper;

// import jakarta.annotation.security.RolesAllowed;   // <- descomentar cuando exista seguridad (login)

import java.time.LocalDate;
import java.util.List;

// Adaptador de entrada REST para los gastos del negocio (COP)
@Path("/api/gastos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Gastos", description = "Registro y consulta de gastos del negocio")
// @RolesAllowed("ADMIN")   // <- protege TODOS los endpoints de esta clase
public class GastoResource {

    @Inject
    GestionarGastosUseCase gastos;

    // Registra un gasto y responde 201
    @POST
    @Operation(summary = "Registrar un gasto")
    public Response registrar(@Valid @NotNull(message = "Los datos del gasto son obligatorios")
                              GastoRequest request) {
        Gasto gasto = gastos.registrar(request.nombre(), request.monto(), request.fechaGasto(), request.categoria());
        return Response.status(Response.Status.CREATED).entity(GastoRestMapper.toResponse(gasto)).build();
    }

    // Actualiza un gasto (404 si no existe)
    @PUT
    @Path("/{id}")
    @Operation(summary = "Actualizar un gasto")
    public GastoResponse actualizar(@PathParam("id") Long id,
                                    @Valid @NotNull(message = "Los datos del gasto son obligatorios")
                                    GastoRequest request) {
        Gasto gasto = gastos.actualizar(id, request.nombre(), request.monto(), request.fechaGasto(),
                request.categoria());
        return GastoRestMapper.toResponse(gasto);
    }

    // Elimina un gasto y responde 204
    @DELETE
    @Path("/{id}")
    @Operation(summary = "Eliminar un gasto")
    public Response eliminar(@PathParam("id") Long id) {
        gastos.eliminar(id);
        return Response.noContent().build();
    }

    // Busca un gasto por id (404 si no existe)
    @GET
    @Path("/{id}")
    @Operation(summary = "Buscar un gasto por id")
    public GastoResponse buscarPorId(@PathParam("id") Long id) {
        return GastoRestMapper.toResponse(gastos.buscarPorId(id));
    }

    // Lista todos los gastos
    @GET
    @Operation(summary = "Listar todos los gastos")
    public List<GastoResponse> listar() {
        return GastoRestMapper.toResponseList(gastos.listar());
    }

    // Gastos en un rango de fechas: /api/gastos/rango?inicio=2026-11-01&fin=2026-11-30
    @GET
    @Path("/rango")
    @Operation(summary = "Listar los gastos en un rango de fechas")
    public List<GastoResponse> listarPorRango(@QueryParam("inicio")
                                              @NotNull(message = "La fecha inicial es obligatoria") LocalDate inicio,
                                              @QueryParam("fin")
                                              @NotNull(message = "La fecha final es obligatoria") LocalDate fin) {
        return GastoRestMapper.toResponseList(gastos.listarPorRango(inicio, fin));
    }
}
