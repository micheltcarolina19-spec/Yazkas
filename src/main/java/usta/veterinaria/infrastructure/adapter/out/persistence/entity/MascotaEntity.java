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
import usta.veterinaria.domain.model.TipoMascota;
import usta.veterinaria.domain.model.Sexo;
import usta.veterinaria.domain.model.Pelaje;
import usta.veterinaria.domain.model.NivelAgresividad;
import usta.veterinaria.domain.model.Energia;

import java.time.LocalDate;

// Entidad JPA que representa la tabla "mascotas"
@Entity
@Table(name = "mascotas")
@Getter
@Setter
public class MascotaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMascota tipo;

    private String raza;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sexo sexo;

    private Float tamano;

    private Float peso;

    private String color;

    @Enumerated(EnumType.STRING)
    private Pelaje pelaje;

    @Column(length = 1000)
    private String observaciones;

    @Column(name = "foto_mascota")
    private String fotoMascota;

    private String temperamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_agresividad")
    private NivelAgresividad nivelAgresividad;

    @Column(name = "usa_bozal")
    private Boolean usaBozal;

    @Column(length = 1000)
    private String fobias;

    private Boolean sociable;

    @Enumerated(EnumType.STRING)
    private Energia energia;

    // Relación: muchas mascotas pueden pertenecer a un mismo usuario
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;
}