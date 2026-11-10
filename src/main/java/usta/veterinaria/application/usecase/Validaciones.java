package usta.veterinaria.application.usecase;

import usta.veterinaria.domain.exception.BusinessRuleException;

import java.time.LocalDate;

// Validaciones de entrada que se repiten en los casos de uso. Evitan consultar los repositorios
// con valores nulos o rangos inválidos.
public final class Validaciones {

    private Validaciones() {
    }

    public static Long requerirId(Long id, String nombre) {
        if (id == null) {
            throw new BusinessRuleException("El " + nombre + " es obligatorio");
        }
        return id;
    }

    public static LocalDate requerirFecha(LocalDate fecha, String nombre) {
        if (fecha == null) {
            throw new BusinessRuleException("La " + nombre + " es obligatoria");
        }
        return fecha;
    }

    public static void rangoFechas(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new BusinessRuleException("Las fechas de inicio y fin son obligatorias");
        }
        if (inicio.isAfter(fin)) {
            throw new BusinessRuleException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
    }
}
