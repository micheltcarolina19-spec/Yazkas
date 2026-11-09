package usta.veterinaria.infrastructure.adapter.in.rest.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

// Convierte un valor con formato incorrecto en un 400. Casos comunes en la peluquería:
//   - Un enum que no existe:   "tipo": "LORO"            -> "Debe ser uno de estos valores: GATO, PERRO"
//   - Una fecha mal escrita:   "fechaAgendamiento": "20/11/2026"
//   - Una hora mal escrita:    "horaAgendamiento": "9am"
//   - Un número como texto:    "precioVenta": "cincuenta mil"
@Provider
public class InvalidFormatExceptionMapper implements ExceptionMapper<InvalidFormatException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(InvalidFormatException exception) {
        String campo = JsonErrorDetails.campo(exception);
        String esperado = JsonErrorDetails.tipoEsperado(exception.getTargetType());

        String mensaje = "El valor '" + exception.getValue() + "' no es válido"
                + JsonErrorDetails.enCampo(campo);

        return ErrorResponseFactory.construir(
                Response.Status.BAD_REQUEST.getStatusCode(),
                ErrorResponseFactory.DATOS_INVALIDOS,
                mensaje,
                uriInfo,
                List.of((campo == null ? "valor" : campo) + ": debe ser " + esperado));
    }
}
