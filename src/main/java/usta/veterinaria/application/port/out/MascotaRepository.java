package usta.veterinaria.application.port.out;

import usta.veterinaria.domain.model.Mascota;

import java.util.List;
import java.util.Optional;

// Puerto secundario: persistencia de mascotas
public interface MascotaRepository {

    Mascota save(Mascota mascota);

    Optional<Mascota> findById(Long id);

    List<Mascota> findAll();

    List<Mascota> findByUsuarioId(Long usuarioId);

    long countByUsuarioId(Long usuarioId);

    void deleteById(Long id);
}
