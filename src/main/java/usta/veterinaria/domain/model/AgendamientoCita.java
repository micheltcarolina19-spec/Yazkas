package usta.veterinaria.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/** Clase que representa el agendamiento de una cita en la veterinaria. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AgendamientoCita {

    private Long id;
    private BigDecimal precioTotal;
    private EstadoCita estado;
}
