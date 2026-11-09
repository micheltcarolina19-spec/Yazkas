// =====================================================================================================
// =====================================================================================================
// ===                                                                                               ===
// ===   ERROR DE LA INTELIGENCIA ARTIFICIAL (OpenAI)  -> 503 SERVICIO NO DISPONIBLE                  ===
// ===                                                                                               ===
// =====================================================================================================
// =====================================================================================================
//
// Este archivo está TODO COMENTADO a propósito para que el proyecto compile mientras no exista la
// excepción ServicioExternoException. Se activa en 2 pasos:
//
// -----------------------------------------------------------------------------------------------------
// PASO 1. Crear el archivo:  application/exception/ServicioExternoException.java
//         (al lado de ResourceNotFoundException.java) y pegar esto, quitando los "//":
// -----------------------------------------------------------------------------------------------------
//
// package usta.veterinaria.application.exception;
//
// // Excepción que se lanza cuando un servicio externo (por ejemplo la IA que estima la duración
// // de la cita) no responde o responde algo que no se puede usar
// public class ServicioExternoException extends RuntimeException {
//
//     private static final long serialVersionUID = 1L;
//
//     public ServicioExternoException(String message) {
//         super(message);
//     }
//
//     public ServicioExternoException(String message, Throwable cause) {
//         super(message, cause);
//     }
// }
//
// -----------------------------------------------------------------------------------------------------
// PASO 2. Quitar los "//" del código de abajo (desde "package" hasta la última llave) en ESTE archivo.
// -----------------------------------------------------------------------------------------------------
//
// ¿Quién la lanza? El adaptador de IA (adapter/out/ia/...) que implementa EstimadorDuracionCitaPort,
// cuando OpenAI no responde, se demora demasiado o responde algo distinto a ALTA, MEDIA o BAJA:
//
//     throw new ServicioExternoException(
//             "No fue posible estimar la duración de la cita en este momento. Intente más tarde", e);
//
// =====================================================================================================

// package usta.veterinaria.infrastructure.adapter.in.rest.exception;
//
// import jakarta.ws.rs.core.Context;
// import jakarta.ws.rs.core.Response;
// import jakarta.ws.rs.core.UriInfo;
// import jakarta.ws.rs.ext.ExceptionMapper;
// import jakarta.ws.rs.ext.Provider;
// import org.jboss.logging.Logger;
// import usta.veterinaria.application.exception.ServicioExternoException;
//
// // Convierte una falla de un servicio externo (la IA) en un 503 (Service Unavailable).
// // El cliente ve un mensaje amable; el error técnico queda en el log.
// @Provider
// public class ServicioExternoExceptionMapper implements ExceptionMapper<ServicioExternoException> {
//
//     private static final Logger LOG = Logger.getLogger(ServicioExternoExceptionMapper.class);
//
//     @Context
//     UriInfo uriInfo;
//
//     @Override
//     public Response toResponse(ServicioExternoException exception) {
//         LOG.error("Falló un servicio externo", exception);
//         return ErrorResponseFactory.construir(
//                 Response.Status.SERVICE_UNAVAILABLE.getStatusCode(),
//                 ErrorResponseFactory.SERVICIO_NO_DISPONIBLE,
//                 exception.getMessage(),
//                 uriInfo);
//     }
// }

// =====================================================================================================
// ===                       FIN DEL BLOQUE DEL ERROR DE LA INTELIGENCIA ARTIFICIAL                  ===
// =====================================================================================================
