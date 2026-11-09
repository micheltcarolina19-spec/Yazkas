package usta.veterinaria.infrastructure.adapter.in.rest.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

// Traduce al español los errores HTTP que lanza el propio framework (JAX-RS), conservando su código:
//   404 ruta que no existe, 405 método no permitido, 415 no se envió JSON, 401/403 seguridad, etc.
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    private static final Logger LOG = Logger.getLogger(WebApplicationExceptionMapper.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(WebApplicationException exception) {
        int estado = exception.getResponse() == null
                ? Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()
                : exception.getResponse().getStatus();

        if (estado >= 500) {
            LOG.error("Error HTTP " + estado + " en el API", exception);
        }

        return ErrorResponseFactory.construir(estado, titulo(estado), mensaje(estado), uriInfo);
    }

    private String titulo(int estado) {
        Response.Status status = Response.Status.fromStatusCode(estado);
        return switch (estado) {
            case 400 -> "Solicitud incorrecta";
            case 401 -> "No autenticado";
            case 403 -> "Acceso denegado";
            case 404 -> ErrorResponseFactory.NO_ENCONTRADO;
            case 405 -> "Método no permitido";
            case 406 -> "Formato de respuesta no soportado";
            case 409 -> ErrorResponseFactory.CONFLICTO;
            case 415 -> "Tipo de contenido no soportado";
            case 503 -> ErrorResponseFactory.SERVICIO_NO_DISPONIBLE;
            default -> status != null ? status.getReasonPhrase() : "Error HTTP " + estado;
        };
    }

    private String mensaje(int estado) {
        return switch (estado) {
            case 400 -> "La solicitud tiene parámetros inválidos (revise los valores de la URL, por ejemplo las fechas)";
            case 401 -> "Debe iniciar sesión para realizar esta acción";
            case 403 -> "No tiene permisos para realizar esta acción";
            case 404 -> "No se encontró la ruta o el recurso solicitado";
            case 405 -> "El método HTTP usado no está permitido para esta ruta";
            case 406 -> "El API solo responde en formato JSON";
            case 409 -> "La solicitud entra en conflicto con los datos existentes";
            case 415 -> "El cuerpo de la solicitud debe enviarse en formato JSON (Content-Type: application/json)";
            case 503 -> "El servicio no está disponible en este momento. Intente más tarde";
            default -> estado >= 500 ? ErrorResponseFactory.MENSAJE_ERROR_INTERNO : "No se pudo procesar la solicitud";
        };
    }
}
