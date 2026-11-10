package usta.veterinaria.infrastructure.adapter.out.persistence.entity.mascota;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

// Entidad JPA que representa la tabla "vacunaciones"
@Entity
@Table(name = "vacunaciones")
@Getter
@Setter
public class VacunacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_vacuna", nullable = false, length = 150)
    private String nombreVacuna;

    @Column(name = "fecha_aplicacion", nullable = false)
    private LocalDate fechaAplicacion;

    @Column(name = "fecha_refuerzo")
    private LocalDate fechaRefuerzo;

    @Column(name = "numero_lote", length = 100)
    private String numeroLote;

    @Column(name = "numero_dosis", nullable = false)
    private Integer numeroDosis;

    @Column(length = 1000)
    private String observaciones;

    // Muchas vacunaciones pueden pertenecer a una misma mascota
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mascota_id", nullable = false)
    private MascotaEntity mascota;
}