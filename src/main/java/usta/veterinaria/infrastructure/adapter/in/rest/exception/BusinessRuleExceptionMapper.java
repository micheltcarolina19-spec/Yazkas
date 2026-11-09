package usta.veterinaria.infrastructure.adapter.in.rest.exception;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import usta.veterinaria.domain.exception.BusinessRuleException;

// Convierte una regla de negocio incumplida del dominio en un error 400 (Bad Request).
// El mensaje ya viene claro desde el dominio, por ejemplo:
// "No hay cupo para Toby en ese horario. El siguiente espacio disponible es 2026-11-10T11:00"
@Provider
public class BusinessRuleExceptionMapper implements ExceptionMapper<BusinessRuleException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(BusinessRuleException exception) {
        return ErrorResponseFactory.construir(
                Response.Status.BAD_REQUEST.getStatusCode(),
                ErrorResponseFactory.REGLA_DE_NEGOCIO,
                exception.getMessage(),
                uriInfo);
    }
}
