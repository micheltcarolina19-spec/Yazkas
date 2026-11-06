package usta.veterinaria.application.port.out;

import usta.veterinaria.domain.model.DuracionCita;
import usta.veterinaria.domain.model.Mascota;

// =====================================================================================
// =====================================================================================
// ===   PUERTO DE SALIDA: ESTIMACIÓN DE LA DURACIÓN DE LA CITA CON IA               ===
// =====================================================================================
// =====================================================================================
//
// Ubicación recomendada: application/port/out
//   - Es un puerto de SALIDA porque la aplicación es la que "sale" a pedirle algo
//     a un sistema externo (la API de inteligencia artificial).
//   - Los puertos de ENTRADA (port/in) son los casos de uso que llaman los controladores.
//
// Quién lo implementa: un adaptador en infrastructure (por ejemplo
// infrastructure/ai/EstimadorDuracionCitaIaAdapter) que arma el prompt con:
//   tipo, raza, tamano, peso y pelaje de la mascota,
// y le pide a la IA que responda SOLO una de estas palabras: ALTA, MEDIA o BAJA.
//
// Criterios del levantamiento de requerimientos (para el prompt):
//   - BAJA  : perro pequeño y pelaje corto              -> máximo 2 horas
//   - MEDIA : perro mediano y pelaje corto              -> entre 2 y 4 horas
//   - ALTA  : perro grande y pelaje largo               -> más de 4 horas
//
// Flujo del caso de uso (application/service):
//   1. citaService.validarDatosParaEstimacion(mascota);          // dominio
//   2. DuracionCita duracion = estimadorDuracion.estimar(mascota); // este puerto -> IA
//   3. mascotaCita.setDuracionCita(duracion);
//   4. citaService.agendar(nuevasCitas, citasExistentes);         // dominio
//
// El cliente solo ve si la duración es ALTA, MEDIA o BAJA, nunca un precio.
// =====================================================================================
public interface EstimadorDuracionCitaPort {

    // Devuelve la duración estimada (ALTA, MEDIA o BAJA) según la raza y el pelaje de la mascota
    DuracionCita estimar(Mascota mascota);
}
// =====================================================================================
// ===   FIN PUERTO DE ESTIMACIÓN DE DURACIÓN                                         ===
// =====================================================================================
