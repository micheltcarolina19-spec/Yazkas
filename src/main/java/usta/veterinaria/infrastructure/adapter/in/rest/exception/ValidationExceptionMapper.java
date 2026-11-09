package usta.veterinaria.infrastructure.adapter.in.rest.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

// Convierte los errores de validación de los DTO (@NotBlank, @Size, @Positive...) en un 400.
// Devuelve TODOS los campos con error en "detalles", por ejemplo:
//   ["mascota.nombre: El nombre de la mascota es obligatorio",
//    "detalles[0].cantidad: La cantidad debe ser mayor a 0"]
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<String> detalles = exception.getConstraintViolations().stream()
                .map(violacion -> nombreCampo(violacion) + ": " + violacion.getMessage())
                .sorted()
                .toList();

        String mensaje = detalles.size() == 1
                ? "La solicitud tiene 1 dato inválido"
                : "La solicitud tiene " + detalles.size() + " datos inválidos";

        return ErrorResponseFactory.construir(
                Response.Status.BAD_REQUEST.getStatusCode(),
                ErrorResponseFactory.DATOS_INVALIDOS,
                mensaje,
                uriInfo,
                detalles);
    }

    // Arma el nombre del campo sin el método ni el parámetro del resource.
    // "crear.request.mascota.nombre" -> "mascota.nombre" ; "agendar.request.mascotaIds[0]" -> "mascotaIds[0]"
    private String nombreCampo(ConstraintViolation<?> violacion) {
        StringBuilder campo = new StringBuilder();
        String parametro = null;

        for (Path.Node nodo : violacion.getPropertyPath()) {
            ElementKind tipo = nodo.getKind();
            if (tipo == ElementKind.METHOD) {
                continue;
            }
            if (tipo == ElementKind.PARAMETER) {
                parametro = nodo.getName();
                continue;
            }
            // Posición dentro de una lista: detalles[0]
            if (nodo.isInIterable()) {
                Object posicion = nodo.getIndex() != null ? nodo.getIndex() : nodo.getKey();
                campo.append('[').append(posicion == null ? "" : posicion).append(']');
            }
            // Los elementos de una lista no tienen nombre propio ("<list element>")
            if (tipo == ElementKind.CONTAINER_ELEMENT || nodo.getName() == null) {
                continue;
            }
            if (!campo.isEmpty()) {
                campo.append('.');
            }
            campo.append(nodo.getName());
        }

        if (!campo.isEmpty()) {
            return campo.toString();
        }
        // La validación fue sobre el parámetro completo (por ejemplo, el body llegó vacío)
        return parametro != null ? parametro : "solicitud";
    }
}
