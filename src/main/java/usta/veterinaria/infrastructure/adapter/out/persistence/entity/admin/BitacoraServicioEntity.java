package usta.veterinaria.infrastructure.adapter.out.persistence.entity.admin;

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
import usta.veterinaria.domain.model.admin.TipoBano;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.usuario.AgendamientoCitaEntity;

import java.math.BigDecimal;

// Entidad JPA que representa la tabla "bitacora_servicios"
@Entity
@Table(name = "bitacora_servicios")
@Getter
@Setter
public class BitacoraServicioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_servicio", nullable = false)
    private String tipoServicio;

    @Column(name = "foto_salida")
    private String fotoSalida;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_bano")
    private TipoBano tipoBano;

    @Column(name = "precio_subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioSubtotal;

    @Column(name = "observaciones_peluqueria", length = 1000)
    private String observacionesPeluqueria;

    // Relación: muchos servicios pueden pertenecer a una misma cita
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agendamiento_cita_id", nullable = false)
    private AgendamientoCitaEntity agendamientoCita;
}