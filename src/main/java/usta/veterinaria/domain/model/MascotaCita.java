package usta.veterinaria.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalTime;

/** Clase asociativa entre Mascota y AgendamientoCita. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
/** Clase asociativa entre Mascota y AgendamientoCita (una cita por mascota dentro de cada agendamiento). */
public class MascotaCita {

    private Long id;
    // Mascota que asiste a la cita
    private Mascota mascota;
    // Agendamiento al que pertenece la cita
    private AgendamientoCita agendamientoCita;
    private LocalDate fechaCreacion;
    private LocalDate fechaAgendamiento;
    private LocalTime horaAgendamiento;
    private DuracionCita duracionCita;
}