package usta.veterinaria.application.port.out;

import usta.veterinaria.domain.model.Desparasitacion;

import java.util.List;

// Puerto secundario: persistencia de desparasitaciones aplicadas
public interface DesparasitacionRepository {

    Desparasitacion save(Desparasitacion desparasitacion);

    List<Desparasitacion> findByMascotaId(Long mascotaId);
}
