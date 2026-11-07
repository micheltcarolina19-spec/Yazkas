package usta.veterinaria.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import usta.veterinaria.domain.model.DuracionCita;

import java.time.LocalDate;
import java.time.LocalTime;

// Entidad JPA que representa la tabla "mascota_citas"
@Entity
@Table(name = "mascota_citas",
        uniqueConstraints = @UniqueConstraint(columnNames = {"mascota_id", "agendamiento_cita_id"}))
@Getter
@Setter
public class MascotaCitaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación: una mascota puede tener varias citas
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mascota_id", nullable = false)
    private MascotaEntity mascota;

    // Relación: un agendamiento puede estar asociado a varias mascotas
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agendamiento_cita_id", nullable = false)
    private AgendamientoCitaEntity agendamientoCita;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDate fechaCreacion;

    @Column(name = "fecha_agendamiento", nullable = false)
    private LocalDate fechaAgendamiento;

    @Column(name = "hora_agendamiento", nullable = false)
    private LocalTime horaAgendamiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "duracion_cita", nullable = false)
    private DuracionCita duracionCita;
}