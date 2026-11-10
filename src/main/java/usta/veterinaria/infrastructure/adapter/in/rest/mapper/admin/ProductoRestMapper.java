package usta.veterinaria.infrastructure.adapter.in.rest.mapper.admin;

import usta.veterinaria.domain.model.mascota.Desparasitacion;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.domain.model.admin.Producto;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.admin.BitacoraServicioResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.admin.ProductoCatalogoResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.admin.ProductoResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.mascota.DesparasitacionResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.MapperUtils;

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
    public static BitacoraServicioResponse.GananciaProductoResponse toGananciaResponse(Long productoId, BigDecimal gananciaUnitaria) {
        return new BitacoraServicioResponse.GananciaProductoResponse(productoId, gananciaUnitaria);
    }

    // Convierte una desparasitación del dominio a la respuesta REST
    public static final class DesparasitacionRestMapper {

        private DesparasitacionRestMapper() {
        }

        public static DesparasitacionResponse toResponse(Desparasitacion desparasitacion) {
            Mascota mascota = desparasitacion.getMascota();
            return new DesparasitacionResponse(
                    desparasitacion.getId(),
                    mascota == null ? null : mascota.getId(),
                    mascota == null ? null : mascota.getNombre(),
                    desparasitacion.getMarca(),
                    desparasitacion.getFechaAplicacion(),
                    MapperUtils.nombre(desparasitacion.getTipo()),
                    desparasitacion.getNumeroLote(),
                    desparasitacion.getObservaciones());
        }

        public static List<DesparasitacionResponse> toResponseList(List<Desparasitacion> desparasitaciones) {
            return MapperUtils.lista(desparasitaciones, DesparasitacionRestMapper::toResponse);
        }
    }
}
