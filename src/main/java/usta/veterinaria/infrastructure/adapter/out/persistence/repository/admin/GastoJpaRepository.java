package usta.veterinaria.infrastructure.adapter.out.persistence.repository.admin;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.admin.GastoEntity;

import java.time.LocalDate;
import java.util.List;

// Repositorio Panache de gastos: aquí viven las consultas con nombre
@ApplicationScoped
public class GastoJpaRepository implements PanacheRepository<GastoEntity> {

    // Gastos con fecha entre inicio y fin (ambas inclusive), ordenados por fecha
    public List<GastoEntity> findByFechaGastoBetween(LocalDate inicio, LocalDate fin) {
        return list("fechaGasto between ?1 and ?2 order by fechaGasto", inicio, fin);
    }
}