package usta.veterinaria.application.port.in.usuario;

import usta.veterinaria.domain.model.usuario.AgendamientoCita;
import usta.veterinaria.domain.model.usuario.DuracionCita;
import usta.veterinaria.domain.model.usuario.MascotaCita;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

// Puerto primario: agendamiento y ciclo de vida de las citas
public interface GestionarCitasUseCase {

    // Agenda una cita para una o varias mascotas del mismo usuario en la misma fecha y hora
    List<MascotaCita> agendar(Long usuarioId, List<Long> mascotaIds, LocalDate fechaAgendamiento,
                              LocalTime horaAgendamiento);

    List<MascotaCita> reprogramar(Long agendamientoId, LocalDate nuevaFecha, LocalTime nuevaHora);

    AgendamientoCita cancelar(Long agendamientoId);

    // Completa la cita con los servicios registrados en la bitácora y calcula el precio total
    AgendamientoCita completar(Long agendamientoId);

    LocalDateTime siguienteEspacioDisponible(Long mascotaId, LocalDateTime horaDeseada);

    // Duración estimada por la IA para cada mascota (valida que sean del usuario). No se muestra precio.
    Map<Long, DuracionCita> estimarDuraciones(Long usuarioId, List<Long> mascotaIds);

    // Horarios del día para agendar JUNTAS las mascotas indicadas (máximo 2 a la vez).
    // Si reprogramarAgendamientoId no es null, se ignoran las citas de ese agendamiento (se está moviendo)
    // y se usan las duraciones que ya tiene guardadas.
    // Mapa ordenado: hora de inicio -> null si está libre, o el motivo (CitaService.NO_DISPONIBLE_*)
    Map<LocalTime, String> disponibilidad(Long usuarioId, List<Long> mascotaIds, LocalDate fecha,
                                          Long reprogramarAgendamientoId);

    AgendamientoCita buscarPorId(Long agendamientoId);

    List<MascotaCita> listarCitasDelAgendamiento(Long agendamientoId);

    List<MascotaCita> listarCitasPorMascota(Long mascotaId);

    List<MascotaCita> listarCitasPorRango(LocalDate inicio, LocalDate fin);
}