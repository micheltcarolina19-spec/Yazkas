package usta.veterinaria.infrastructure.adapter.in.rest.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

// Arma la respuesta HTTP de error con el mismo formato para todos los mappers
final class ErrorResponseFactory {

    // Títulos que se muestran en el campo "error"
    static final String DATOS_INVALIDOS = "Datos inválidos";
    static final String JSON_INVALIDO = "JSON inválido";
    static final String REGLA_DE_NEGOCIO = "Regla de negocio incumplida";
    static final String NO_ENCONTRADO = "Recurso no encontrado";
    static final String CONFLICTO = "Conflicto con los datos existentes";
    static final String SERVICIO_NO_DISPONIBLE = "Servicio no disponible";
    static final String ERROR_INTERNO = "Error interno del servidor";

    static final String MENSAJE_ERROR_INTERNO =
            "Ocurrió un error inesperado. Intente de nuevo y, si el problema continúa, contacte al administrador";

    private static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    private ErrorResponseFactory() {
    }

    static Response construir(int estado, String error, String mensaje, UriInfo uriInfo) {
        return construir(estado, error, mensaje, uriInfo, List.of());
    }

    static Response construir(int estado, String error, String mensaje, UriInfo uriInfo, List<String> detalles) {
        ErrorResponse cuerpo = new ErrorResponse(
                estado,
                error,
                mensaje,
                ruta(uriInfo),
                LocalDateTime.now(ZONA_COLOMBIA),
                detalles == null ? List.of() : List.copyOf(detalles));
        return Response.status(estado)
                .type(MediaType.APPLICATION_JSON)
                .entity(cuerpo)
                .build();
    }

    // Ruta que pidió el cliente, por ejemplo /api/mascotas/5
    private static String ruta(UriInfo uriInfo) {
        try {
            return uriInfo == null ? null : uriInfo.getRequestUri().getPath();
        } catch (RuntimeException e) {
            // Fuera de una petición HTTP no hay UriInfo disponible
            return null;
        }
    }
}
