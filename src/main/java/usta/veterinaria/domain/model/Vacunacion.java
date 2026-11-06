package usta.veterinaria.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/** Clase que representa una vacuna aplicada a una mascota. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Vacunacion {

    private Long id;
    private String nombreVacuna;
    private LocalDate fechaAplicacion;
    private LocalDate fechaRefuerzo;
    private String numeroLote;
    private Integer numeroDosis;
    private String observaciones;
    // Mascota a la que se le aplicó la vacuna
    private Mascota mascota;
}
