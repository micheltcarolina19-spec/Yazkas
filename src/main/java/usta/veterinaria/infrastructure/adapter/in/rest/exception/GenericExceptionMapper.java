package usta.veterinaria.infrastructure.adapter.in.rest.exception;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import usta.veterinaria.application.exception.ResourceNotFoundException;
import usta.veterinaria.domain.exception.BusinessRuleException;

// Último respaldo: atrapa cualquier error que no manejen los demás mappers.
//
// A veces el error real llega "envuelto" en otro (por ejemplo, cuando falla el commit de una
// transacción @Transactional). Por eso primero se busca la causa original:
//   - BusinessRuleException      -> 400
//   - ResourceNotFoundException  -> 404
//   - Error de PostgreSQL        -> 409 / 400 / 503 (ver ErroresBaseDatos)
// Si no se reconoce, responde 500 con un mensaje genérico (sin mostrar detalles técnicos al
// cliente) y deja el error completo en el log para revisarlo.
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GenericExceptionMapper.class);
    private static final int MAXIMO_CAUSAS = 15;

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Exception exception) {
        Throwable actual = exception;
        int vueltas = 0;
        while (actual != null && vueltas++ < MAXIMO_CAUSAS) {
            if (actual instanceof BusinessRuleException regla) {
                return ErrorResponseFactory.construir(
                        Response.Status.BAD_REQUEST.getStatusCode(),
                        ErrorResponseFactory.REGLA_DE_NEGOCIO,
                        regla.getMessage(),
                        uriInfo);
            }
            if (actual instanceof ResourceNotFoundException noEncontrado) {
                return ErrorResponseFactory.construir(
                        Response.Status.NOT_FOUND.getStatusCode(),
                        ErrorResponseFactory.NO_ENCONTRADO,
                        noEncontrado.getMessage(),
                        uriInfo);
            }
            actual = actual.getCause();
        }

        return ErroresBaseDatos.traducir(exception, uriInfo).orElseGet(() -> {
            LOG.error("Error no controlado en el API", exception);
            return ErrorResponseFactory.construir(
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    ErrorResponseFactory.ERROR_INTERNO,
                    ErrorResponseFactory.MENSAJE_ERROR_INTERNO,
                    uriInfo);
        });
    }
}
