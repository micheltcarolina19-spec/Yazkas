package usta.veterinaria.infrastructure.adapter.out.persistence.repository.mascota;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.mascota.HistorialClinicaEntity;

import java.util.Optional;

// Repositorio Panache de historias clínicas: aquí viven las consultas con nombre
@ApplicationScoped
public class HistorialClinicaJpaRepository implements PanacheRepository<HistorialClinicaEntity> {

    // Historia clínica de una mascota (como máximo existe una).
    // Trae la mascota cargada porque el mapper la convierte al dominio.
    public Optional<HistorialClinicaEntity> findByMascotaId(Long mascotaId) {
        return find("select h from HistorialClinicaEntity h join fetch h.mascota m where m.id = ?1", mascotaId)
                .firstResultOptional();
    }
}