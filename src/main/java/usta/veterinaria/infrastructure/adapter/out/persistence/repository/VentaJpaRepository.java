package usta.veterinaria.infrastructure.adapter.out.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.VentaEntity;

import java.time.LocalDate;
import java.util.List;

// Repositorio Panache de ventas: consultas por fecha
@ApplicationScoped
public class VentaJpaRepository implements PanacheRepository<VentaEntity> {

    // Ventas dentro de un rango de fechas (ambos extremos incluidos), de la más reciente a la más antigua
    public List<VentaEntity> findByFechaVentaBetween(LocalDate inicio, LocalDate fin) {
        return list("fechaVenta between ?1 and ?2 order by fechaVenta desc, id desc", inicio, fin);
    }
}