package usta.veterinaria.infrastructure.adapter.out.persistence.repository.admin;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.admin.PlanillaSanidadEntity;

import java.time.LocalDate;
import java.util.List;

// Repositorio Panache de la planilla de sanidad: consultas por fecha
@ApplicationScoped
public class PlanillaSanidadJpaRepository implements PanacheRepository<PlanillaSanidadEntity> {

    // Planillas de un día
    public List<PlanillaSanidadEntity> findByFechaPlanilla(LocalDate fecha) {
        return list("fechaPlanilla = ?1 order by id", fecha);
    }

    // Planillas dentro de un rango de fechas (ambos extremos incluidos)
    public List<PlanillaSanidadEntity> findByFechaPlanillaBetween(LocalDate inicio, LocalDate fin) {
        return list("fechaPlanilla between ?1 and ?2 order by fechaPlanilla, id", inicio, fin);
    }
}