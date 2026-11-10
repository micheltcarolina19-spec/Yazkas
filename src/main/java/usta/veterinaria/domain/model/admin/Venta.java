package usta.veterinaria.domain.model.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Clase que representa una venta de productos. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Venta {

    private Long id;
    private LocalDate fechaVenta;
    private BigDecimal totalVenta;
}
