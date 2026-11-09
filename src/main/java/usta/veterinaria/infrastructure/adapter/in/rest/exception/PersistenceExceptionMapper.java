package usta.veterinaria.infrastructure.adapter.in.rest.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

// Convierte los errores de JPA/Hibernate en respuestas claras:
//   - Correo duplicado, producto con ventas que se intenta eliminar, etc. -> 409 (ver ErroresBaseDatos)
//   - Registro modificado al mismo tiempo por otra persona                -> 409
//   - Entidad que ya no existe                                            -> 404
//   - Cualquier otro error de base de datos                               -> 500 (se registra en el log)
@Provider
public class PersistenceExceptionMapper implements ExceptionMapper<PersistenceException> {

    private static final Logger LOG = Logger.getLogger(PersistenceExceptionMapper.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(PersistenceException exception) {
        if (exception instanceof OptimisticLockException) {
            return ErrorResponseFactory.construir(
                    Response.Status.CONFLICT.getStatusCode(),
                    ErrorResponseFactory.CONFLICTO,
                    "El registro fue modificado por otra persona mientras lo editaba. Recargue e intente de nuevo",
                    uriInfo);
        }
        if (exception instanceof EntityNotFoundException) {
            return ErrorResponseFactory.construir(
                    Response.Status.NOT_FOUND.getStatusCode(),
                    ErrorResponseFactory.NO_ENCONTRADO,
                    "El registro solicitado ya no existe",
                    uriInfo);
        }

        return ErroresBaseDatos.traducir(exception, uriInfo).orElseGet(() -> {
            LOG.error("Error de base de datos no controlado", exception);
            return ErrorResponseFactory.construir(
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    ErrorResponseFactory.ERROR_INTERNO,
                    ErrorResponseFactory.MENSAJE_ERROR_INTERNO,
                    uriInfo);
        });
    }
}
