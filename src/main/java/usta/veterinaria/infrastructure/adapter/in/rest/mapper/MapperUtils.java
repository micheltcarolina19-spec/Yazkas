package usta.veterinaria.infrastructure.adapter.in.rest.mapper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

// Utilidades de conversión compartidas por los mappers REST (solo conversión, sin lógica de negocio)
final class MapperUtils {

    private MapperUtils() {
    }

    // Nombre del enum para la respuesta, o null si no tiene valor
    static String nombre(Enum<?> valor) {
        return valor == null ? null : valor.name();
    }

    // Convierte una lista completa; si llega null devuelve una lista vacía
    static <T, R> List<R> lista(Collection<T> origen, Function<T, R> conversor) {
        if (origen == null) {
            return List.of();
        }
        return origen.stream().map(conversor).toList();
    }

    // Convierte un mapa con llaves enum a un mapa con el nombre del enum como llave, conservando el orden
    static <E extends Enum<E>, V> Map<String, V> mapaPorNombre(Map<E, V> origen) {
        Map<String, V> resultado = new LinkedHashMap<>();
        if (origen != null) {
            origen.forEach((llave, valor) -> resultado.put(llave.name(), valor));
        }
        return resultado;
    }
}
