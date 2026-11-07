package usta.veterinaria.application.port.out;

import usta.veterinaria.domain.model.PlanillaSanidad;

import java.time.LocalDate;
import java.util.List;

// Puerto secundario: persistencia de la planilla de limpieza y desinfección
public interface PlanillaSanidadRepository {

    PlanillaSanidad save(PlanillaSanidad planilla);

    List<PlanillaSanidad> findByFechaPlanilla(LocalDate fecha);

    List<PlanillaSanidad> findByFechaPlanillaBetween(LocalDate inicio, LocalDate fin);
}
