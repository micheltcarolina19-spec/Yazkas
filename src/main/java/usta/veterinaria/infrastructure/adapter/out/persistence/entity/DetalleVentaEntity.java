package usta.veterinaria.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

// Entidad JPA que representa la tabla "detalle_venta"
@Entity
@Table(name = "detalle_venta",
        uniqueConstraints = @UniqueConstraint(columnNames = {"venta_id", "producto_id"}))
@Getter
@Setter
public class DetalleVentaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación: muchos detalles pueden pertenecer a una misma venta
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private VentaEntity venta;

    // Relación: un producto puede aparecer en diferentes detalles de venta
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private ProductoEntity producto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false)
    private Integer cantidad;
}