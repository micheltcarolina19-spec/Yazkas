package usta.veterinaria.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalTime;

/** Clase asociativa entre Mascota y AgendamientoCita (clave compuesta: mascota + agendamientoCita). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class MascotaCita {

    // Mascota que asiste a la cita
    private Mascota mascota;
    // Agendamiento al que pertenece la cita
    private AgendamientoCita agendamientoCita;
    private LocalDate fechaCreacion;
    private LocalDate fechaAgendamiento;
    private LocalTime horaAgendamiento;
    private DuracionCita duracionCita;
}
