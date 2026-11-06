package usta.veterinaria.domain.service;

import usta.veterinaria.domain.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

// Utilidad para manejar montos en pesos colombianos (COP).
// El peso colombiano no maneja centavos, por eso todos los montos se guardan como enteros.
public final class MonedaColombiana {

    private static final Locale LOCALE_COLOMBIA = Locale.forLanguageTag("es-CO");

    private MonedaColombiana() {
        // Clase utilitaria: no se instancia
    }

    // Redondea un monto a pesos enteros (sin centavos)
    public static BigDecimal normalizar(BigDecimal valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return valor.setScale(0, RoundingMode.HALF_UP);
    }

    // Valida que el monto exista y sea mayor a cero, y lo deja en pesos enteros
    public static BigDecimal validarMontoPositivo(BigDecimal valor, String campo) {
        if (valor == null) {
            throw new BusinessRuleException("El campo " + campo + " es obligatorio");
        }
        BigDecimal normalizado = normalizar(valor);
        if (normalizado.signum() <= 0) {
            throw new BusinessRuleException("El campo " + campo + " debe ser mayor a $0 COP");
        }
        return normalizado;
    }

    // Da formato de pesos colombianos, por ejemplo: $ 1.250.000 COP
    public static String formatear(BigDecimal valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(LOCALE_COLOMBIA);
        formato.setMaximumFractionDigits(0);
        formato.setMinimumFractionDigits(0);
        return "$ " + formato.format(normalizar(valor)) + " COP";
    }
}
