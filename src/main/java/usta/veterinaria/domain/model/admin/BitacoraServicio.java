package usta.veterinaria.domain.model.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import usta.veterinaria.domain.model.usuario.AgendamientoCita;

import java.math.BigDecimal;

/** Clase que representa el registro de un servicio prestado en una cita. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BitacoraServicio {

    private Long id;
    private String tipoServicio;
    private String fotoSalida;
    private TipoBano tipoBano;
    private BigDecimal precioSubtotal;
    private String observacionesPeluqueria;
    // Cita a la que pertenece el servicio
    private AgendamientoCita agendamientoCita;
}
