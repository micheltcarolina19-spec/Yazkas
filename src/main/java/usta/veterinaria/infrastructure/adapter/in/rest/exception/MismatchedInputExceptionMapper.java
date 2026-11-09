package usta.veterinaria.infrastructure.adapter.in.rest.exception;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.PropertyBindingException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

// Convierte un JSON con la estructura equivocada en un 400. Casos comunes:
//   - El body llegó vacío.
//   - Se mandó un texto donde va una lista:    "mascotaIds": "1"     (debe ser [1])
//   - Se mandó una lista donde va un objeto:    "mascota": [ ... ]
//   - Se mandó un campo que no existe en el DTO (solo si Jackson está configurado para rechazarlos)
// Los valores con formato incorrecto (enum, fecha, número) los maneja InvalidFormatExceptionMapper.
@Provider
public class MismatchedInputExceptionMapper implements ExceptionMapper<MismatchedInputException> {

    // Texto que pone Jackson cuando el body llega vacío
    private static final String SIN_CONTENIDO = "No content to map";

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(MismatchedInputException exception) {
        String campo = JsonErrorDetails.campo(exception);

        if (exception instanceof PropertyBindingException propiedad) {
            return error("El campo '" + propiedad.getPropertyName() + "' no existe en esta solicitud",
                    List.of());
        }
        if (campo == null && exception.getOriginalMessage() != null
                && exception.getOriginalMessage().startsWith(SIN_CONTENIDO)) {
            return error("El cuerpo de la solicitud está vacío; envíe los datos en formato JSON", List.of());
        }
        if (campo == null) {
            return error("El cuerpo de la solicitud no tiene la estructura esperada",
                    List.of("Se esperaba " + JsonErrorDetails.tipoEsperado(exception.getTargetType())));
        }
        return error("El tipo de dato es incorrecto" + JsonErrorDetails.enCampo(campo),
                List.of(campo + ": debe ser " + JsonErrorDetails.tipoEsperado(exception.getTargetType())));
    }

    private Response error(String mensaje, List<String> detalles) {
        return ErrorResponseFactory.construir(
                Response.Status.BAD_REQUEST.getStatusCode(),
                ErrorResponseFactory.JSON_INVALIDO,
                mensaje,
                uriInfo,
                detalles);
    }
}
