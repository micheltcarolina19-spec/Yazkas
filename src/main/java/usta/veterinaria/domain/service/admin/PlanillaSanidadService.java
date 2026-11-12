package usta.veterinaria.domain.service.admin;

import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.admin.DiaSanidad;
import usta.veterinaria.domain.model.admin.PlanillaSanidad;
import usta.veterinaria.domain.service.RelojColombia;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

// Servicio que valida la planilla de limpieza y desinfección (de lunes a viernes)
public class PlanillaSanidadService {

    public static final int MAX_OBSERVACIONES = 255;

    private final Clock clock;

    public PlanillaSanidadService() {
        this(RelojColombia.reloj());
    }

    public PlanillaSanidadService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "El reloj es obligatorio");
    }

    // Valida y normaliza una fila de la planilla (sirve para crear y para editar).
    // El día se calcula a partir de la fecha.
    public PlanillaSanidad registrar(PlanillaSanidad planilla, List<PlanillaSanidad> registrosExistentes) {
        if (planilla == null) {
            throw new BusinessRuleException("El registro de sanidad es obligatorio");
        }
        LocalDate hoy = LocalDate.now(clock);
        if (planilla.getFechaPlanilla() == null) {
            planilla.setFechaPlanilla(hoy);
        }
        if (planilla.getFechaPlanilla().isAfter(hoy)) {
            throw new BusinessRuleException("No se puede registrar la planilla de una fecha futura");
        }
        if (planilla.getAreaLimpieza() == null || planilla.getHerramienta() == null) {
            throw new BusinessRuleException("Debe indicar el área y la herramienta o superficie limpiada");
        }

        planilla.setDiaSanidad(diaDe(planilla.getFechaPlanilla()));

        boolean limpieza = Boolean.TRUE.equals(planilla.getLimpiezaRealizada());
        boolean desinfeccion = Boolean.TRUE.equals(planilla.getDesinfeccion());
        if (desinfeccion && !limpieza) {
            throw new BusinessRuleException("No se puede marcar desinfección sin haber realizado la limpieza");
        }
        planilla.setLimpiezaRealizada(limpieza);
        planilla.setDesinfeccion(desinfeccion);

        planilla.setObservaciones(normalizarObservaciones(planilla.getObservaciones()));

        if (registrosExistentes != null) {
            for (PlanillaSanidad otro : registrosExistentes) {
                if (otro != null && !esElMismoRegistro(planilla, otro)
                        && planilla.getFechaPlanilla().equals(otro.getFechaPlanilla())
                        && planilla.getAreaLimpieza() == otro.getAreaLimpieza()
                        && planilla.getHerramienta() == otro.getHerramienta()) {
                    throw new BusinessRuleException("Ya existe un registro de " + planilla.getHerramienta()
                            + " en " + planilla.getAreaLimpieza() + " para esa fecha");
                }
            }
        }
        return planilla;
    }

    // Al editar, el registro que viene de la base de datos es otro objeto con el mismo id:
    // no debe contarse como duplicado de sí mismo
    private boolean esElMismoRegistro(PlanillaSanidad planilla, PlanillaSanidad otro) {
        return otro == planilla
                || (planilla.getId() != null && planilla.getId().equals(otro.getId()));
    }

    private String normalizarObservaciones(String observaciones) {
        if (observaciones == null || observaciones.isBlank()) {
            return null;
        }
        String limpio = observaciones.trim();
        if (limpio.length() > MAX_OBSERVACIONES) {
            throw new BusinessRuleException("Las observaciones no pueden superar los "
                    + MAX_OBSERVACIONES + " caracteres");
        }
        return limpio;
    }

    // Porcentaje de registros con limpieza y desinfección completas (ej: 80.00 = 80 %)
    public BigDecimal porcentajeCumplimiento(List<PlanillaSanidad> registros) {
        if (registros == null || registros.isEmpty()) {
            return BigDecimal.ZERO;
        }
        long completos = registros.stream()
                .filter(r -> r != null
                        && Boolean.TRUE.equals(r.getLimpiezaRealizada())
                        && Boolean.TRUE.equals(r.getDesinfeccion()))
                .count();
        return BigDecimal.valueOf(completos * 100)
                .divide(BigDecimal.valueOf(registros.size()), 2, RoundingMode.HALF_UP);
    }

    // Convierte la fecha al día de la planilla; solo se trabaja de lunes a viernes
    public DiaSanidad diaDe(LocalDate fecha) {
        DayOfWeek dia = fecha.getDayOfWeek();
        return switch (dia) {
            case MONDAY -> DiaSanidad.LUNES;
            case TUESDAY -> DiaSanidad.MARTES;
            case WEDNESDAY -> DiaSanidad.MIERCOLES;
            case THURSDAY -> DiaSanidad.JUEVES;
            case FRIDAY -> DiaSanidad.VIERNES;
            case SATURDAY, SUNDAY -> throw new BusinessRuleException(
                    "La planilla de sanidad solo se diligencia de lunes a viernes");
        };
    }
}