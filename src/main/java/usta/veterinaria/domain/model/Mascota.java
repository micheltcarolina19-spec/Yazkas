package usta.veterinaria.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/** Clase que representa una mascota registrada por un usuario. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Mascota {

    private Long id;
    private String nombre;
    private LocalDate fechaNacimiento;
    private TipoMascota tipo;
    private String raza;
    private Sexo sexo;
    private Float tamano;
    private Float peso;
    private String color;
    private Pelaje pelaje;
    private String observaciones;
    private String fotoMascota;
    private String temperamento;
    private NivelAgresividad nivelAgresividad;
    private Boolean usaBozal;
    private String fobias;
    private Boolean sociable;
    private Energia energia;
    // Propietario de la mascota
    private Usuario usuario;
}
