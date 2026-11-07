package usta.veterinaria.infrastructure.adapter.out.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.VacunacionEntity;

import java.util.List;

// Repositorio Panache de vacunaciones: consultas con la mascota y su propietario cargados,
// porque el mapper convierte mascota (y su usuario) al dominio
@ApplicationScoped
public class VacunacionJpaRepository implements PanacheRepository<VacunacionEntity> {

    private static final String SELECT_CON_MASCOTA =
            "select v from VacunacionEntity v join fetch v.mascota m join fetch m.usuario ";

    // Vacunaciones de una mascota, la más reciente primero
    public List<VacunacionEntity> findByMascotaId(Long mascotaId) {
        return list(SELECT_CON_MASCOTA + "where m.id = ?1 order by v.fechaAplicacion desc, v.id desc", mascotaId);
    }

    // Vacunaciones que tienen fecha de refuerzo, las más próximas primero
    public List<VacunacionEntity> findConRefuerzo() {
        return list(SELECT_CON_MASCOTA + "where v.fechaRefuerzo is not null order by v.fechaRefuerzo, v.id");
    }
}