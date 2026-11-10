package usta.veterinaria.infrastructure.adapter.in.rest.resource.admin;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
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
import usta.veterinaria.application.port.in.admin.RegistrarVentaUseCase;
import usta.veterinaria.domain.model.admin.Venta;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.admin.CumplimientoSanidadResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.admin.VentaRestMapper;

// import jakarta.annotation.security.RolesAllowed;   // <- descomentar cuando exista seguridad (login)

import java.time.LocalDate;
import java.util.List;

// Adaptador de entrada REST para registrar las ventas que el administrador ya hizo en persona
@Path("/api/ventas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Ventas", description = "Registro y consulta de ventas de productos (COP)")
// @RolesAllowed("ADMIN")   // <- protege TODOS los endpoints de esta clase
public class VentaResource {

    @Inject
    RegistrarVentaUseCase ventas;

    // Registra la venta (subtotal = precio de venta x cantidad) y responde 201 con sus líneas
    @POST
    @Operation(summary = "Registrar una venta")
    public Response registrar(@Valid @NotNull(message = "Los datos de la venta son obligatorios")
                              CumplimientoSanidadResponse.VentaRequest request) {
        Venta venta = ventas.registrar(
                VentaRestMapper.toProductoIds(request),
                VentaRestMapper.toCantidades(request),
                request.fechaVenta());
        CumplimientoSanidadResponse.VentaResponse respuesta = VentaRestMapper.toResponse(venta, ventas.listarDetalles(venta.getId()));
        return Response.status(Response.Status.CREATED).entity(respuesta).build();
    }

    // Busca una venta por id con sus líneas (404 si no existe)
    @GET
    @Path("/{id}")
    @Operation(summary = "Buscar una venta por id con sus productos")
    public CumplimientoSanidadResponse.VentaResponse buscarPorId(@PathParam("id") Long id) {
        return VentaRestMapper.toResponse(ventas.buscarPorId(id), ventas.listarDetalles(id));
    }

    // Ventas en un rango de fechas (sin líneas): /api/ventas?inicio=2026-11-01&fin=2026-11-30
    @GET
    @Operation(summary = "Listar las ventas en un rango de fechas")
    public List<CumplimientoSanidadResponse.VentaResponse> listarPorRango(@QueryParam("inicio")
                                              @NotNull(message = "La fecha inicial es obligatoria") LocalDate inicio,
                                                                          @QueryParam("fin")
                                              @NotNull(message = "La fecha final es obligatoria") LocalDate fin) {
        return VentaRestMapper.toResponseList(ventas.listarPorRango(inicio, fin));
    }
}
