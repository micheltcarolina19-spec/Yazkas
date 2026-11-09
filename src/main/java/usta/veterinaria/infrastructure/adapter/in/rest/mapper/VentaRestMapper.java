package usta.veterinaria.infrastructure.adapter.in.rest.mapper;

import usta.veterinaria.domain.model.DetalleVenta;
import usta.veterinaria.domain.model.Producto;
import usta.veterinaria.domain.model.Venta;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.DetalleVentaRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.DetalleVentaResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.VentaRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.VentaResponse;

import java.util.List;

// Convierte entre los DTO de venta y el dominio:
//   - Request  -> las dos listas que pide RegistrarVentaUseCase (productoIds y cantidades, en el mismo orden)
//   - Dominio  -> VentaResponse con sus líneas
public final class VentaRestMapper {

    private VentaRestMapper() {
    }

    // ---------------------------------- Request -> caso de uso ----------------------------------

    // Ids de los productos, en el mismo orden de las líneas de la venta
    public static List<Long> toProductoIds(VentaRequest request) {
        return MapperUtils.lista(request.detalles(), DetalleVentaRequest::productoId);
    }

    // Cantidades, en el mismo orden que toProductoIds
    public static List<Integer> toCantidades(VentaRequest request) {
        return MapperUtils.lista(request.detalles(), DetalleVentaRequest::cantidad);
    }

    // ---------------------------------- Dominio -> Response ----------------------------------

    public static VentaResponse toResponse(Venta venta, List<DetalleVenta> detalles) {
        return new VentaResponse(
                venta.getId(),
                venta.getFechaVenta(),
                venta.getTotalVenta(),
                toDetalleResponseList(detalles));
    }

    // Para listados donde no se cargan las líneas de cada venta
    public static VentaResponse toResponse(Venta venta) {
        return toResponse(venta, List.of());
    }

    public static List<VentaResponse> toResponseList(List<Venta> ventas) {
        return MapperUtils.lista(ventas, VentaRestMapper::toResponse);
    }

    public static DetalleVentaResponse toDetalleResponse(DetalleVenta detalle) {
        Producto producto = detalle.getProducto();
        return new DetalleVentaResponse(
                detalle.getId(),
                producto == null ? null : producto.getId(),
                producto == null ? null : producto.getNombre(),
                detalle.getCantidad(),
                detalle.getSubtotal());
    }

    public static List<DetalleVentaResponse> toDetalleResponseList(List<DetalleVenta> detalles) {
        return MapperUtils.lista(detalles, VentaRestMapper::toDetalleResponse);
    }
}
