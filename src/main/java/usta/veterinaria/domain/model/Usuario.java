package usta.veterinaria.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Clase que representa un usuario del sistema (administrador o cliente). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Usuario {

    private Long id;
    private String nombre;
    private String apellido;
    private String celular;
    private String correo;
    private RolUsuario rol;
    private String contrasena;
}
