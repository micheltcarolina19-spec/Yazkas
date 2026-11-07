package usta.veterinaria.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import usta.veterinaria.domain.model.TipoDesparasitacion;

import java.time.LocalDate;

// Entidad JPA que representa la tabla "desparasitaciones"
@Entity
@Table(name = "desparasitaciones")
@Getter
@Setter
public class DesparasitacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String marca;

    @Column(name = "fecha_aplicacion", nullable = false)
    private LocalDate fechaAplicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDesparasitacion tipo;

    @Column(name = "numero_lote", nullable = false)
    private String numeroLote;

    @Column(length = 1000)
    private String observaciones;

    // Relación: muchas desparasitaciones pueden pertenecer a una mascota
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mascota_id", nullable = false)
    private MascotaEntity mascota;
}