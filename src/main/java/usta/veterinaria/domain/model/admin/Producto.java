package usta.veterinaria.domain.model.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/** Clase que representa un producto del inventario. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Producto {

    private Long id;
    private String nombre;
    private String marca;
    private CategoriaProducto categoria;
    private String descripcion;
    private TipoProducto tipo;
    private BigDecimal precioBase;
    private BigDecimal precioVenta;
    private String fotoProducto;
    private Integer stock;
}
