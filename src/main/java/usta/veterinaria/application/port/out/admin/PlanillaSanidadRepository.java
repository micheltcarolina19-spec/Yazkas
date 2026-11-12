package usta.veterinaria.application.port.out.admin;

import usta.veterinaria.domain.model.admin.PlanillaSanidad;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Puerto secundario: persistencia de la planilla de limpieza y desinfección
public interface PlanillaSanidadRepository {

    PlanillaSanidad save(PlanillaSanidad planilla);

    List<PlanillaSanidad> findByFechaPlanilla(LocalDate fecha);

    List<PlanillaSanidad> findByFechaPlanillaBetween(LocalDate inicio, LocalDate fin);

    // En usta.veterinaria.application.port.out.admin.PlanillaSanidadRepository
    Optional<PlanillaSanidad> findById(Long id);

    void deleteById(Long id);
}
