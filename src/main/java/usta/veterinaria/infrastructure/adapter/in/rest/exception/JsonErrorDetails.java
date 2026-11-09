package usta.veterinaria.infrastructure.adapter.in.rest.exception;

import com.fasterxml.jackson.databind.JsonMappingException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

// Utilidades para explicar en español los errores de lectura del JSON (Jackson)
final class JsonErrorDetails {

    private JsonErrorDetails() {
    }

    // Nombre del campo donde falló el JSON, por ejemplo "detalles[1].cantidad".
    // Devuelve null si el error no está en un campo (por ejemplo, el body completo).
    static String campo(JsonMappingException exception) {
        StringBuilder campo = new StringBuilder();
        for (JsonMappingException.Reference referencia : exception.getPath()) {
            if (referencia.getFieldName() != null) {
                if (!campo.isEmpty()) {
                    campo.append('.');
                }
                campo.append(referencia.getFieldName());
            } else if (referencia.getIndex() >= 0) {
                campo.append('[').append(referencia.getIndex()).append(']');
            }
        }
        return campo.isEmpty() ? null : campo.toString();
    }

    // Describe qué tipo de dato se esperaba, con un ejemplo
    static String tipoEsperado(Class<?> tipo) {
        if (tipo == null) {
            return "un valor válido";
        }
        if (tipo.isEnum()) {
            String valores = Arrays.stream(tipo.getEnumConstants())
                    .map(valor -> ((Enum<?>) valor).name())
                    .collect(Collectors.joining(", "));
            return "uno de estos valores: " + valores;
        }
        if (tipo == LocalDate.class) {
            return "una fecha con formato AAAA-MM-DD (ej: 2026-11-20)";
        }
        if (tipo == LocalTime.class) {
            return "una hora con formato HH:mm o HH:mm:ss (ej: 09:00)";
        }
        if (tipo == LocalDateTime.class) {
            return "una fecha y hora con formato AAAA-MM-DDTHH:mm:ss (ej: 2026-11-20T09:00:00)";
        }
        if (tipo == Integer.class || tipo == int.class || tipo == Long.class || tipo == long.class
                || tipo == Short.class || tipo == short.class || tipo == BigInteger.class) {
            return "un número entero (ej: 3)";
        }
        if (tipo == BigDecimal.class || tipo == Float.class || tipo == float.class
                || tipo == Double.class || tipo == double.class) {
            return "un número (ej: 45000 o 30.5)";
        }
        if (tipo == Boolean.class || tipo == boolean.class) {
            return "true o false";
        }
        if (tipo == String.class) {
            return "un texto entre comillas";
        }
        if (Collection.class.isAssignableFrom(tipo) || tipo.isArray()) {
            return "una lista (ej: [1, 2])";
        }
        return "un objeto JSON con los datos de " + tipo.getSimpleName();
    }

    // Arma el texto "en el campo 'x'" o vacío si no hay campo
    static String enCampo(String campo) {
        return campo == null ? "" : " en el campo '" + campo + "'";
    }
}
