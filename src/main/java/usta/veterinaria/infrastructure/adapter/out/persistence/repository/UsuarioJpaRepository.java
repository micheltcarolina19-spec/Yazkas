package usta.veterinaria.infrastructure.adapter.out.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.UsuarioEntity;

import java.util.List;
import java.util.Optional;

// Repositorio Panache de usuarios
@ApplicationScoped
public class UsuarioJpaRepository implements PanacheRepository<UsuarioEntity> {

    // Usuario por correo (el servicio de dominio ya lo guarda normalizado en minúsculas)
    public Optional<UsuarioEntity> findByCorreo(String correo) {
        return find("correo", correo).firstResultOptional();
    }

    // Todos los usuarios, ordenados por nombre y apellido
    public List<UsuarioEntity> findAllOrdenados() {
        return list("order by nombre, apellido");
    }
}