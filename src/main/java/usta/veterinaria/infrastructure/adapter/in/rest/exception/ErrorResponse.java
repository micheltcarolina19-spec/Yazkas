package usta.veterinaria.infrastructure.adapter.in.rest.exception;

import java.time.LocalDateTime;
import java.util.List;

// Respuesta estándar de error del API REST de la peluquería.
// Ejemplo:
// {
//   "estado": 400,
//   "error": "Datos inválidos",
//   "mensaje": "La solicitud tiene datos inválidos",
//   "ruta": "/api/productos",
//   "fecha": "2026-11-08T14:55:00",
//   "detalles": ["nombre: El nombre del producto es obligatorio"]
// }
public record ErrorResponse(
        int estado,
        String error,
        String mensaje,
        String ruta,
        LocalDateTime fecha,
        List<String> detalles) {
}
