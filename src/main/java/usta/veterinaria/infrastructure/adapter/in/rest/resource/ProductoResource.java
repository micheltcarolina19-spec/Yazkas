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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import usta.veterinaria.application.port.in.GestionarProductosUseCase;
import usta.veterinaria.domain.model.Producto;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.GananciaProductoResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.ProductoRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.ProductoResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.ReabastecerProductoRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.ProductoRestMapper;

// import jakarta.annotation.security.RolesAllowed;   // <- descomentar cuando exista seguridad (login)

import java.util.List;

// Adaptador de entrada REST para el inventario de productos (vista del ADMINISTRADOR, con precios).
// El catálogo público SIN precios está en CatalogoResource (/api/catalogo).
@Path("/api/productos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Productos", description = "Inventario de productos (administrador)")
// @RolesAllowed("ADMIN")   // <- protege TODOS los endpoints de esta clase
public class ProductoResource {

    @Inject
    GestionarProductosUseCase productos;

    // Crea un producto y responde 201
    @POST
    @Operation(summary = "Crear un producto")
    public Response crear(@Valid @NotNull(message = "Los datos del producto son obligatorios")
                          ProductoRequest request) {
        Producto producto = productos.crear(
                request.nombre(), request.marca(), request.categoria(), request.descripcion(), request.tipo(),
                request.precioBase(), request.precioVenta(), request.fotoProducto(), request.stock());
        return Response.status(Response.Status.CREATED).entity(ProductoRestMapper.toResponse(producto)).build();
    }

    // Actualiza un producto (404 si no existe)
    @PUT
    @Path("/{id}")
    @Operation(summary = "Actualizar un producto")
    public ProductoResponse actualizar(@PathParam("id") Long id,
                                       @Valid @NotNull(message = "Los datos del producto son obligatorios")
                                       ProductoRequest request) {
        Producto producto = productos.actualizar(
                id, request.nombre(), request.marca(), request.categoria(), request.descripcion(), request.tipo(),
                request.precioBase(), request.precioVenta(), request.fotoProducto(), request.stock());
        return ProductoRestMapper.toResponse(producto);
    }

    // Suma unidades al inventario cuando llega mercancía
    @PUT
    @Path("/{id}/reabastecer")
    @Operation(summary = "Reabastecer el stock de un producto")
    public ProductoResponse reabastecer(@PathParam("id") Long id,
                                        @Valid @NotNull(message = "La cantidad es obligatoria")
                                        ReabastecerProductoRequest request) {
        return ProductoRestMapper.toResponse(productos.reabastecer(id, request.cantidad()));
    }

    // Elimina un producto y responde 204 (409 si ya está en alguna venta)
    @DELETE
    @Path("/{id}")
    @Operation(summary = "Eliminar un producto")
    public Response eliminar(@PathParam("id") Long id) {
        productos.eliminar(id);
        return Response.noContent().build();
    }

    // Lista todos los productos con precios y stock
    @GET
    @Operation(summary = "Listar todos los productos (con precios)")
    public List<ProductoResponse> listar() {
        return ProductoRestMapper.toResponseList(productos.listar());
    }

    // Busca un producto por id con precios (404 si no existe)
    @GET
    @Path("/{id}")
    @Operation(summary = "Buscar un producto por id (con precios)")
    public ProductoResponse buscarPorId(@PathParam("id") Long id) {
        return ProductoRestMapper.toResponse(productos.buscarPorId(id));
    }

    // Ganancia por unidad del producto (precio de venta - precio base)
    @GET
    @Path("/{id}/ganancia")
    @Operation(summary = "Consultar la ganancia unitaria de un producto")
    public GananciaProductoResponse ganancia(@PathParam("id") Long id) {
        return ProductoRestMapper.toGananciaResponse(id, productos.gananciaUnitaria(id));
    }
}
