package usta.veterinaria.domain.service;

import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.Gasto;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

// Servicio que valida y registra los gastos del negocio (en pesos colombianos)
public class GastoService {

    private final Clock clock;

    public GastoService() {
        this(RelojColombia.reloj());
    }

    public GastoService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "El reloj es obligatorio");
    }

    // Valida el gasto; si no trae fecha se registra con la fecha de hoy
    public Gasto registrar(Gasto gasto) {
        if (gasto == null) {
            throw new BusinessRuleException("El gasto es obligatorio");
        }
        if (gasto.getNombre() == null || gasto.getNombre().isBlank()) {
            throw new BusinessRuleException("El nombre o concepto del gasto es obligatorio");
        }
        if (gasto.getCategoria() == null) {
            throw new BusinessRuleException("La categoría del gasto es obligatoria");
        }

        LocalDate hoy = LocalDate.now(clock);
        if (gasto.getFechaGasto() == null) {
            gasto.setFechaGasto(hoy);
        }
        if (gasto.getFechaGasto().isAfter(hoy)) {
            throw new BusinessRuleException("No se puede registrar un gasto con fecha futura");
        }

        gasto.setNombre(gasto.getNombre().trim());
        gasto.setMonto(MonedaColombiana.validarMontoPositivo(gasto.getMonto(), "monto del gasto"));
        return gasto;
    }
}
