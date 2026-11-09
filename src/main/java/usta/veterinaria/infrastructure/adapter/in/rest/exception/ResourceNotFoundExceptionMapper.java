package usta.veterinaria.infrastructure.adapter.in.rest.exception;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import usta.veterinaria.application.exception.ResourceNotFoundException;

// Convierte un recurso que no existe (mascota, cita, producto, venta...) en un error 404 (Not Found).
// Ejemplo de mensaje: "No existe la mascota con id 15"
@Provider
public class ResourceNotFoundExceptionMapper implements ExceptionMapper<ResourceNotFoundException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ResourceNotFoundException exception) {
        return ErrorResponseFactory.construir(
                Response.Status.NOT_FOUND.getStatusCode(),
                ErrorResponseFactory.NO_ENCONTRADO,
                exception.getMessage(),
                uriInfo);
    }
}
