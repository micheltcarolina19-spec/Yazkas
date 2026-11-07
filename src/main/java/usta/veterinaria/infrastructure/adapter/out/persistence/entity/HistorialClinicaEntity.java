package usta.veterinaria.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

// Entidad JPA que representa la tabla "historias_clinicas"
@Entity
@Table(name = "historias_clinicas")
@Getter
@Setter
public class HistorialClinicaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alergias_medicamentos", length = 1000)
    private String alergiasMedicamentos;

    @Column(name = "alergias_cosmeticas", length = 1000)
    private String alergiasCosmeticas;

    @Column(name = "alergias_alimenticias", length = 1000)
    private String alergiasAlimenticias;

    @Column(name = "enfermedades_cronicas", length = 1000)
    private String enfermedadesCronicas;

    @Column(name = "condiciones_fisicas", length = 1000)
    private String condicionesFisicas;

    @Column(name = "problemas_cardiacos", length = 1000)
    private String problemasCardiacos;

    @Column(name = "problemas_ortopedicos", length = 1000)
    private String problemasOrtopedicos;

    @Column(name = "diagnostico_dermatologico", length = 1000)
    private String diagnosticoDermatologico;

    @Column(name = "presencia_parasitos")
    private Boolean presenciaParasitos;

    @Column(name = "cirugias_recientes", length = 1000)
    private String cirugiasRecientes;

    @Column(length = 1000)
    private String lesiones;

    @Column
    private Boolean esterilizacion;

    @Column(length = 2000)
    private String observaciones;

    // Relación: una historia clínica pertenece a una única mascota
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mascota_id", nullable = false, unique = true)
    private MascotaEntity mascota;
}