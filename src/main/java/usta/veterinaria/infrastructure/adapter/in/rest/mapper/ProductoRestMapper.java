package usta.veterinaria.infrastructure.adapter.in.rest.mapper;

import usta.veterinaria.domain.model.Producto;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.GananciaProductoResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.ProductoCatalogoResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.ProductoResponse;

import java.math.BigDecimal;
import java.util.List;

// Convierte un producto del dominio a las respuestas REST:
//   - toResponse          -> vista del ADMINISTRADOR (con precios y stock)
//   - toCatalogoResponse  -> vista del CLIENTE en la página web (SIN precios)
public final class ProductoRestMapper {

    private ProductoRestMapper() {
    }

    public static ProductoResponse toResponse(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getMarca(),
                MapperUtils.nombre(producto.getCategoria()),
                producto.getDescripcion(),
                MapperUtils.nombre(producto.getTipo()),
                producto.getPrecioBase(),
                producto.getPrecioVenta(),
                producto.getFotoProducto(),
                producto.getStock());
    }

    public static List<ProductoResponse> toResponseList(List<Producto> productos) {
        return MapperUtils.lista(productos, ProductoRestMapper::toResponse);
    }

    // El DTO del catálogo no tiene campos de precio, así que nunca se exponen al cliente
    public static ProductoCatalogoResponse toCatalogoResponse(Producto producto) {
        return new ProductoCatalogoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getMarca(),
                MapperUtils.nombre(producto.getCategoria()),
                producto.getDescripcion(),
                MapperUtils.nombre(producto.getTipo()),
                producto.getFotoProducto(),
                producto.getStock());
    }

    public static List<ProductoCatalogoResponse> toCatalogoResponseList(List<Producto> productos) {
        return MapperUtils.lista(productos, ProductoRestMapper::toCatalogoResponse);
    }

    // Arma la respuesta con la ganancia unitaria que calculó el caso de uso
    public static GananciaProductoResponse toGananciaResponse(Long productoId, BigDecimal gananciaUnitaria) {
        return new GananciaProductoResponse(productoId, gananciaUnitaria);
    }
}
