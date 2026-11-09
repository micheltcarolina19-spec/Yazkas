package usta.veterinaria.infrastructure.adapter.in.rest.exception;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

// Convierte un JSON mal escrito (falta una coma, una llave, comillas...) en un 400.
// Indica la línea y la columna donde está el error para que sea fácil encontrarlo.
@Provider
public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(JsonParseException exception) {
        JsonLocation ubicacion = exception.getLocation();
        List<String> detalles = ubicacion == null
                ? List.of()
                : List.of("Revise la línea " + ubicacion.getLineNr() + ", columna " + ubicacion.getColumnNr());

        return ErrorResponseFactory.construir(
                Response.Status.BAD_REQUEST.getStatusCode(),
                ErrorResponseFactory.JSON_INVALIDO,
                "El cuerpo de la solicitud no es un JSON válido (revise comas, llaves y comillas)",
                uriInfo,
                detalles);
    }
}
