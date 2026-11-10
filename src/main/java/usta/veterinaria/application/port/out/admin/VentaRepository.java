package usta.veterinaria.application.port.out.admin;

import usta.veterinaria.domain.model.admin.Venta;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Puerto secundario: persistencia de ventas
public interface VentaRepository {

    Venta save(Venta venta);

    Optional<Venta> findById(Long id);

    List<Venta> findByFechaVentaBetween(LocalDate inicio, LocalDate fin);
}
