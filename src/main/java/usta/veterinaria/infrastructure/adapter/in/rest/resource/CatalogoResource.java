package usta.veterinaria.infrastructure.adapter.in.rest.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import usta.veterinaria.application.port.in.GestionarProductosUseCase;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.ProductoCatalogoResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.ProductoRestMapper;

import java.util.List;

// Adaptador de entrada REST para el catálogo PÚBLICO de la página web.
// Solo muestra qué productos hay en la peluquería: NUNCA devuelve precios.
@Path("/api/catalogo")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Catalogo", description = "Catálogo público de productos (sin precios)")
public class CatalogoResource {

    @Inject
    GestionarProductosUseCase productos;

    // Lista el catálogo de productos sin precios
    @GET
    @Operation(summary = "Listar el catálogo de productos")
    public List<ProductoCatalogoResponse> listar() {
        return ProductoRestMapper.toCatalogoResponseList(productos.listarCatalogo());
    }

    // Detalle de un producto del catálogo sin precios (404 si no existe)
    @GET
    @Path("/{id}")
    @Operation(summary = "Ver un producto del catálogo")
    public ProductoCatalogoResponse buscarPorId(@PathParam("id") Long id) {
        return ProductoRestMapper.toCatalogoResponse(productos.buscarEnCatalogo(id));
    }
}
