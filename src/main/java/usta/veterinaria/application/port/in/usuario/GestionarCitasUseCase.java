package usta.veterinaria.application.port.in.usuario;

import usta.veterinaria.domain.model.usuario.AgendamientoCita;
import usta.veterinaria.domain.model.usuario.MascotaCita;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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

    AgendamientoCita buscarPorId(Long agendamientoId);

    List<MascotaCita> listarCitasDelAgendamiento(Long agendamientoId);

    List<MascotaCita> listarCitasPorMascota(Long mascotaId);

    List<MascotaCita> listarCitasPorRango(LocalDate inicio, LocalDate fin);
}
