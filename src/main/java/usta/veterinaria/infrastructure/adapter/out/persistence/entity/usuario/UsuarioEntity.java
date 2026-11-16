package usta.veterinaria.infrastructure.adapter.out.persistence.entity.usuario;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import usta.veterinaria.domain.model.usuario.RolUsuario;

// Entidad JPA que representa la tabla "usuarios"
@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, length = 20)
    private String celular;

    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolUsuario rol;

    @Column(name = "contrasena", nullable = false, length = 255)
    private String contrasena;
}