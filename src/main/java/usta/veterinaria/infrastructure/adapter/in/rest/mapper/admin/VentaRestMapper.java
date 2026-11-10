package usta.veterinaria.infrastructure.adapter.in.rest.mapper.admin;

import usta.veterinaria.domain.model.admin.DetalleVenta;
import usta.veterinaria.domain.model.admin.Producto;
import usta.veterinaria.domain.model.admin.Venta;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.admin.CumplimientoSanidadResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.admin.DetalleVentaRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.admin.DetalleVentaResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.MapperUtils;

import java.util.List;

// Convierte entre los DTO de venta y el dominio:
//   - Request  -> las dos listas que pide RegistrarVentaUseCase (productoIds y cantidades, en el mismo orden)
//   - Dominio  -> VentaResponse con sus líneas
public final class VentaRestMapper {

    private VentaRestMapper() {
    }

    // ---------------------------------- Request -> caso de uso ----------------------------------

    // Ids de los productos, en el mismo orden de las líneas de la venta
    public static List<Long> toProductoIds(CumplimientoSanidadResponse.VentaRequest request) {
        return MapperUtils.lista(request.detalles(), DetalleVentaRequest::productoId);
    }

    // Cantidades, en el mismo orden que toProductoIds
    public static List<Integer> toCantidades(CumplimientoSanidadResponse.VentaRequest request) {
        return MapperUtils.lista(request.detalles(), DetalleVentaRequest::cantidad);
    }

    // ---------------------------------- Dominio -> Response ----------------------------------

    public static CumplimientoSanidadResponse.VentaResponse toResponse(Venta venta, List<DetalleVenta> detalles) {
        return new CumplimientoSanidadResponse.VentaResponse(
                venta.getId(),
                venta.getFechaVenta(),
                venta.getTotalVenta(),
                toDetalleResponseList(detalles));
    }

    // Para listados donde no se cargan las líneas de cada venta
    public static CumplimientoSanidadResponse.VentaResponse toResponse(Venta venta) {
        return toResponse(venta, List.of());
    }

    public static List<CumplimientoSanidadResponse.VentaResponse> toResponseList(List<Venta> ventas) {
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
