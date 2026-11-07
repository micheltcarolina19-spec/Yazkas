package usta.veterinaria.application.port.in;

import usta.veterinaria.domain.model.DetalleVenta;
import usta.veterinaria.domain.model.Venta;

import java.time.LocalDate;
import java.util.List;

// Puerto primario: registro y consulta de ventas
public interface RegistrarVentaUseCase {

    // productoIds y cantidades van en paralelo: la cantidad en la posición i corresponde al producto i.
    // Descuenta el stock de cada producto. Si fechaVenta es null se usa la fecha de hoy.
    Venta registrar(List<Long> productoIds, List<Integer> cantidades, LocalDate fechaVenta);

    Venta buscarPorId(Long ventaId);

    List<DetalleVenta> listarDetalles(Long ventaId);

    List<Venta> listarPorRango(LocalDate inicio, LocalDate fin);
}
