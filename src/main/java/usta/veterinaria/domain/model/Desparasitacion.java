package usta.veterinaria.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/** Clase que representa una desparasitación aplicada a una mascota. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Desparasitacion {

    private Long id;
    private String marca;
    private LocalDate fechaAplicacion;
    private TipoDesparasitacion tipo;
    private String numeroLote;
    private String observaciones;
    // Mascota a la que se le aplicó la desparasitación
    private Mascota mascota;
}
