package usta.veterinaria.application.port.out;

import usta.veterinaria.domain.model.Vacunacion;

import java.util.List;

// Puerto secundario: persistencia de vacunas aplicadas
public interface VacunacionRepository {

    Vacunacion save(Vacunacion vacunacion);

    List<Vacunacion> findByMascotaId(Long mascotaId);

    // Vacunas que tienen fecha de refuerzo (para los recordatorios)
    List<Vacunacion> findConRefuerzo();
}
