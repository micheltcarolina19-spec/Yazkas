package usta.veterinaria.application.port.in.admin;

import usta.veterinaria.domain.model.admin.AreaLimpieza;
import usta.veterinaria.domain.model.admin.Herramienta;
import usta.veterinaria.domain.model.admin.PlanillaSanidad;

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
