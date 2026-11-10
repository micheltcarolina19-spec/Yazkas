package usta.veterinaria.application.port.out.admin;

import usta.veterinaria.domain.model.admin.DetalleVenta;

import java.time.LocalDate;
import java.util.List;

// Puerto secundario: persistencia de los detalles (líneas) de una venta.
// Los detalles deben traer cargados la venta y el producto.
public interface DetalleVentaRepository {

    List<DetalleVenta> saveAll(List<DetalleVenta> detalles);

    List<DetalleVenta> findByVentaId(Long ventaId);

    // Detalles cuya venta tiene fecha entre inicio y fin (ambas inclusive)
    List<DetalleVenta> findByVentaFechaVentaBetween(LocalDate inicio, LocalDate fin);
}
