package usta.veterinaria.infrastructure.adapter.in.rest.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.sql.SQLException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Traduce los errores técnicos de PostgreSQL a mensajes claros para el usuario.
// Usa el SQLState estándar de PostgreSQL, por ejemplo:
//   23505 -> valor duplicado (ej: el correo ya está registrado)                     -> 409
//   23503 -> llave foránea (ej: eliminar un producto que ya está en una venta)       -> 409
//   23502 -> falta un dato obligatorio                                               -> 400
//   22001 -> texto más largo de lo permitido                                         -> 400
//   08xxx -> no hay conexión con la base de datos                                    -> 503
final class ErroresBaseDatos {

    private static final String VALOR_DUPLICADO = "23505";
    private static final String LLAVE_FORANEA = "23503";
    private static final String DATO_OBLIGATORIO = "23502";
    private static final String RESTRICCION_CHECK = "23514";
    private static final String TEXTO_MUY_LARGO = "22001";
    private static final String NUMERO_FUERA_DE_RANGO = "22003";
    private static final String CONFLICTO_CONCURRENCIA = "40001";
    private static final String BLOQUEO_MUTUO = "40P01";
    private static final String PREFIJO_CONEXION = "08";

    // PostgreSQL puede responder en inglés o en español según la configuración del servidor
    private static final Pattern LLAVE_Y_VALOR = Pattern.compile("(?i)(?:key|llave) \\(([^)]+)\\)=\\(([^)]*)\\)");
    private static final Pattern TABLA_QUE_REFERENCIA =
            Pattern.compile("(?:referenced from table|referida desde la tabla) [\"«]([\\w.]+)[\"»]");
    private static final Pattern COLUMNA = Pattern.compile("(?:column|columna) [\"«]([\\w.]+)[\"»]");
    private static final String TODAVIA_REFERENCIADO_EN = "still referenced";
    private static final String TODAVIA_REFERIDA_ES = "todavía es referida";

    private ErroresBaseDatos() {
    }

    // Si en la cadena de causas hay un error de base de datos, devuelve la respuesta traducida
    static Optional<Response> traducir(Throwable exception, UriInfo uriInfo) {
        SQLException sql = buscarSqlException(exception);
        if (sql == null || sql.getSQLState() == null) {
            return Optional.empty();
        }
        String estado = sql.getSQLState();
        String texto = sql.getMessage() == null ? "" : sql.getMessage();

        if (VALOR_DUPLICADO.equals(estado)) {
            Matcher llave = LLAVE_Y_VALOR.matcher(texto);
            String mensaje = llave.find()
                    ? "Ya existe un registro con " + llave.group(1) + " = '" + llave.group(2) + "'"
                    : "Ya existe un registro con esos mismos datos";
            return Optional.of(conflicto(mensaje, uriInfo));
        }
        if (LLAVE_FORANEA.equals(estado)) {
            return Optional.of(conflicto(mensajeLlaveForanea(texto), uriInfo));
        }
        if (DATO_OBLIGATORIO.equals(estado)) {
            Matcher columna = COLUMNA.matcher(texto);
            String mensaje = columna.find()
                    ? "Falta un dato obligatorio: " + columna.group(1)
                    : "Falta un dato obligatorio";
            return Optional.of(datosInvalidos(mensaje, uriInfo));
        }
        if (TEXTO_MUY_LARGO.equals(estado)) {
            return Optional.of(datosInvalidos("Uno de los textos enviados supera el tamaño permitido", uriInfo));
        }
        if (NUMERO_FUERA_DE_RANGO.equals(estado)) {
            return Optional.of(datosInvalidos("Uno de los valores numéricos es demasiado grande", uriInfo));
        }
        if (RESTRICCION_CHECK.equals(estado)) {
            return Optional.of(datosInvalidos("Uno de los datos no cumple las restricciones de la base de datos", uriInfo));
        }
        if (CONFLICTO_CONCURRENCIA.equals(estado) || BLOQUEO_MUTUO.equals(estado)) {
            return Optional.of(conflicto(
                    "Otra persona estaba modificando la misma información. Intente de nuevo", uriInfo));
        }
        if (estado.startsWith(PREFIJO_CONEXION)) {
            return Optional.of(ErrorResponseFactory.construir(
                    Response.Status.SERVICE_UNAVAILABLE.getStatusCode(),
                    ErrorResponseFactory.SERVICIO_NO_DISPONIBLE,
                    "No hay conexión con la base de datos en este momento. Intente más tarde",
                    uriInfo));
        }
        return Optional.empty();
    }

    // Diferencia entre "no se puede eliminar porque se usa en otra tabla" y "lo que referencia no existe"
    private static String mensajeLlaveForanea(String texto) {
        if (texto.contains(TODAVIA_REFERENCIADO_EN) || texto.contains(TODAVIA_REFERIDA_ES)) {
            Matcher tabla = TABLA_QUE_REFERENCIA.matcher(texto);
            return tabla.find()
                    ? "No se puede eliminar el registro porque tiene información relacionada en '" + tabla.group(1) + "'"
                    : "No se puede eliminar el registro porque tiene información relacionada";
        }
        Matcher llave = LLAVE_Y_VALOR.matcher(texto);
        return llave.find()
                ? "El registro relacionado no existe (" + llave.group(1) + " = " + llave.group(2) + ")"
                : "El registro relacionado no existe";
    }

    private static SQLException buscarSqlException(Throwable exception) {
        Throwable actual = exception;
        int vueltas = 0;
        while (actual != null && vueltas++ < 15) {
            if (actual instanceof SQLException sql) {
                return sql;
            }
            actual = actual.getCause();
        }
        return null;
    }

    private static Response conflicto(String mensaje, UriInfo uriInfo) {
        return ErrorResponseFactory.construir(
                Response.Status.CONFLICT.getStatusCode(), ErrorResponseFactory.CONFLICTO, mensaje, uriInfo);
    }

    private static Response datosInvalidos(String mensaje, UriInfo uriInfo) {
        return ErrorResponseFactory.construir(
                Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponseFactory.DATOS_INVALIDOS, mensaje, uriInfo);
    }
}
