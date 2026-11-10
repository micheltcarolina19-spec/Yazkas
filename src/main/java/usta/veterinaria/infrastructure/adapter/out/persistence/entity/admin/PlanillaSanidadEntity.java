package usta.veterinaria.infrastructure.adapter.out.persistence.entity.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import usta.veterinaria.domain.model.admin.DiaSanidad;
import usta.veterinaria.domain.model.admin.AreaLimpieza;
import usta.veterinaria.domain.model.admin.Herramienta;

import java.time.LocalDate;

// Entidad JPA que representa la tabla "planillas_sanidad"
@Entity
@Table(name = "planillas_sanidad")
@Getter
@Setter
public class PlanillaSanidadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_planilla", nullable = false)
    private LocalDate fechaPlanilla;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_sanidad", nullable = false)
    private DiaSanidad diaSanidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "area_limpieza", nullable = false)
    private AreaLimpieza areaLimpieza;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Herramienta herramienta;

    @Column(name = "limpieza_realizada", nullable = false)
    private Boolean limpiezaRealizada;

    @Column(nullable = false)
    private Boolean desinfeccion;
}