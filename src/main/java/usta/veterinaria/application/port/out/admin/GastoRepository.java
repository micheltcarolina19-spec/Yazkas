package usta.veterinaria.application.port.out.admin;

import usta.veterinaria.domain.model.admin.Gasto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Puerto secundario: persistencia de gastos
public interface GastoRepository {

    Gasto save(Gasto gasto);

    Optional<Gasto> findById(Long id);

    List<Gasto> findAll();

    List<Gasto> findByFechaGastoBetween(LocalDate inicio, LocalDate fin);

    void deleteById(Long id);
}
