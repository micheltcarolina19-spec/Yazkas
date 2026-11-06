package usta.veterinaria.domain.service;

import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.Desparasitacion;
import usta.veterinaria.domain.model.Vacunacion;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Servicio con las reglas del carnet de la mascota: vacunas y desparasitaciones
public class SaludMascotaService {

    private final Clock clock;

    public SaludMascotaService() {
        this(RelojColombia.reloj());
    }

    public SaludMascotaService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "El reloj es obligatorio");
    }

    // Valida una vacuna aplicada
    public Vacunacion registrarVacunacion(Vacunacion vacunacion) {
        if (vacunacion == null || vacunacion.getMascota() == null) {
            throw new BusinessRuleException("La vacuna debe pertenecer a una mascota");
        }
        if (vacunacion.getNombreVacuna() == null || vacunacion.getNombreVacuna().isBlank()) {
            throw new BusinessRuleException("El nombre de la vacuna es obligatorio");
        }
        validarFechaAplicacion(vacunacion.getFechaAplicacion());
        if (vacunacion.getFechaRefuerzo() != null
                && !vacunacion.getFechaRefuerzo().isAfter(vacunacion.getFechaAplicacion())) {
            throw new BusinessRuleException("La fecha de refuerzo debe ser posterior a la fecha de aplicación");
        }
        if (vacunacion.getNumeroDosis() == null) {
            vacunacion.setNumeroDosis(1);
        }
        if (vacunacion.getNumeroDosis() < 1) {
            throw new BusinessRuleException("El número de dosis debe ser 1 o mayor");
        }
        vacunacion.setNombreVacuna(vacunacion.getNombreVacuna().trim());
        return vacunacion;
    }

    // Valida una desparasitación aplicada
    public Desparasitacion registrarDesparasitacion(Desparasitacion desparasitacion) {
        if (desparasitacion == null || desparasitacion.getMascota() == null) {
            throw new BusinessRuleException("La desparasitación debe pertenecer a una mascota");
        }
        if (desparasitacion.getMarca() == null || desparasitacion.getMarca().isBlank()) {
            throw new BusinessRuleException("La marca del desparasitante es obligatoria");
        }
        if (desparasitacion.getTipo() == null) {
            throw new BusinessRuleException("Debe indicar el tipo de desparasitación");
        }
        validarFechaAplicacion(desparasitacion.getFechaAplicacion());
        desparasitacion.setMarca(desparasitacion.getMarca().trim());
        return desparasitacion;
    }

    // Vacunas cuyo refuerzo toca dentro de los próximos "dias" días (para recordarle al dueño)
    public List<Vacunacion> refuerzosProximos(List<Vacunacion> vacunas, int dias) {
        if (dias < 0) {
            throw new BusinessRuleException("Los días deben ser cero o más");
        }
        LocalDate hoy = LocalDate.now(clock);
        LocalDate limite = hoy.plusDays(dias);
        List<Vacunacion> resultado = new ArrayList<>();
        if (vacunas != null) {
            for (Vacunacion vacuna : vacunas) {
                LocalDate refuerzo = vacuna == null ? null : vacuna.getFechaRefuerzo();
                if (refuerzo != null && !refuerzo.isBefore(hoy) && !refuerzo.isAfter(limite)) {
                    resultado.add(vacuna);
                }
            }
        }
        return resultado;
    }

    // Vacunas cuyo refuerzo ya se pasó
    public List<Vacunacion> refuerzosVencidos(List<Vacunacion> vacunas) {
        LocalDate hoy = LocalDate.now(clock);
        List<Vacunacion> resultado = new ArrayList<>();
        if (vacunas != null) {
            for (Vacunacion vacuna : vacunas) {
                if (vacuna != null && vacuna.getFechaRefuerzo() != null && vacuna.getFechaRefuerzo().isBefore(hoy)) {
                    resultado.add(vacuna);
                }
            }
        }
        return resultado;
    }

    private void validarFechaAplicacion(LocalDate fecha) {
        if (fecha == null) {
            throw new BusinessRuleException("La fecha de aplicación es obligatoria");
        }
        if (fecha.isAfter(LocalDate.now(clock))) {
            throw new BusinessRuleException("La fecha de aplicación no puede ser futura");
        }
    }
}
