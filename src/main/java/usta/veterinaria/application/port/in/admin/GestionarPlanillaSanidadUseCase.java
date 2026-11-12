package usta.veterinaria.application.port.in.admin;

import usta.veterinaria.domain.model.admin.AreaLimpieza;
import usta.veterinaria.domain.model.admin.Herramienta;
import usta.veterinaria.domain.model.admin.PlanillaSanidad;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Puerto de entrada: CRUD de la planilla de limpieza y desinfección
public interface GestionarPlanillaSanidadUseCase {

    PlanillaSanidad registrar(LocalDate fecha, AreaLimpieza areaLimpieza, Herramienta herramienta,
                              Boolean limpiezaRealizada, Boolean desinfeccion, String observaciones);

    PlanillaSanidad actualizar(Long id, LocalDate fecha, AreaLimpieza areaLimpieza, Herramienta herramienta,
                               Boolean limpiezaRealizada, Boolean desinfeccion, String observaciones);

    void eliminar(Long id);

    PlanillaSanidad buscarPorId(Long id);

    List<PlanillaSanidad> listarPorFecha(LocalDate fecha);

    List<PlanillaSanidad> listarPorRango(LocalDate inicio, LocalDate fin);

    BigDecimal porcentajeCumplimiento(LocalDate inicio, LocalDate fin);
}