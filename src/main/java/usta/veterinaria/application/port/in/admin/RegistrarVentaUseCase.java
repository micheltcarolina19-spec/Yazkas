package usta.veterinaria.application.port.in.admin;

import usta.veterinaria.domain.model.admin.DetalleVenta;
import usta.veterinaria.domain.model.admin.Venta;

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

    /** Arma el ticket del punto de venta sin guardar: precio y stock reales, subtotal con la regla del dominio. */
    List<DetalleVenta> armarTicket(List<Long> productoIds, List<Integer> cantidades);
}
