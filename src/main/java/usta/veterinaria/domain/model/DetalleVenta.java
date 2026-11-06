package usta.veterinaria.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/** Clase que representa el detalle (línea) de una venta (clave compuesta: venta + producto). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class DetalleVenta {

    // Venta a la que pertenece el detalle
    private Venta venta;
    // Producto vendido
    private Producto producto;
    private BigDecimal subtotal;
    private Integer cantidad;
}
