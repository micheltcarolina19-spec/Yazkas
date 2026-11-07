package usta.veterinaria.application.port.in;

import usta.veterinaria.domain.model.AreaLimpieza;
import usta.veterinaria.domain.model.Herramienta;
import usta.veterinaria.domain.model.PlanillaSanidad;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Puerto primario: planilla de limpieza y desinfección
public interface GestionarPlanillaSanidadUseCase {

    // Si fecha es null se usa la fecha de hoy
    PlanillaSanidad registrar(LocalDate fecha, AreaLimpieza areaLimpieza, Herramienta herramienta,
                              Boolean limpiezaRealizada, Boolean desinfeccion);

    List<PlanillaSanidad> listarPorFecha(LocalDate fecha);

    List<PlanillaSanidad> listarPorRango(LocalDate inicio, LocalDate fin);

    BigDecimal porcentajeCumplimiento(LocalDate inicio, LocalDate fin);
}
