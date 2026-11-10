package usta.veterinaria.domain.model.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/** Clase que representa un registro de la planilla de limpieza y desinfección. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PlanillaSanidad {

    private Long id;
    private LocalDate fechaPlanilla;
    private DiaSanidad diaSanidad;
    private AreaLimpieza areaLimpieza;
    private Herramienta herramienta;
    private Boolean limpiezaRealizada;
    private Boolean desinfeccion;
}
