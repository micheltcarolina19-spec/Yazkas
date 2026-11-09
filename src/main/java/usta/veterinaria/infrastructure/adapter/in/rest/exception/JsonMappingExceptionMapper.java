package usta.veterinaria.infrastructure.adapter.in.rest.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

// Respaldo para cualquier otro error al convertir el JSON en un DTO que no cubren
// InvalidFormatExceptionMapper ni MismatchedInputExceptionMapper. Responde 400.
@Provider
public class JsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(JsonMappingException exception) {
        String campo = JsonErrorDetails.campo(exception);
        return ErrorResponseFactory.construir(
                Response.Status.BAD_REQUEST.getStatusCode(),
                ErrorResponseFactory.JSON_INVALIDO,
                "No se pudieron leer los datos enviados" + JsonErrorDetails.enCampo(campo),
                uriInfo);
    }
}
