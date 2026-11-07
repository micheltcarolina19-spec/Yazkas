package usta.veterinaria.application.port.out;

import usta.veterinaria.domain.model.Producto;

import java.util.List;
import java.util.Optional;

// Puerto secundario: persistencia de productos del inventario
public interface ProductoRepository {

    Producto save(Producto producto);

    Optional<Producto> findById(Long id);

    List<Producto> findAll();

    void deleteById(Long id);
}
