package usta.veterinaria.domain.model.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Clase que representa un gasto del negocio. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Gasto {

    private Long id;
    private String nombre;
    private BigDecimal monto;
    private LocalDate fechaGasto;
    private CategoriaGasto categoria;
}
