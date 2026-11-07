package usta.veterinaria.infrastructure.adapter.out.persistence.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.port.out.PlanillaSanidadRepository;
import usta.veterinaria.domain.model.PlanillaSanidad;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.PlanillaSanidadEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.mapper.PlanillaSanidadEntityMapper;
import usta.veterinaria.infrastructure.adapter.out.persistence.repository.PlanillaSanidadJpaRepository;

import java.time.LocalDate;
import java.util.List;

// Adaptador JPA del puerto PlanillaSanidadRepository: guarda/lee las planillas en MySQL
@ApplicationScoped
public class JpaPlanillaSanidadRepository implements PlanillaSanidadRepository {

    @Inject
    PlanillaSanidadJpaRepository repository;

    // Guarda la planilla (inserta o actualiza)
    @Override
    @Transactional
    public PlanillaSanidad save(PlanillaSanidad planilla) {
        PlanillaSanidadEntity saved = repository.getEntityManager()
                .merge(PlanillaSanidadEntityMapper.toEntity(planilla));
        return PlanillaSanidadEntityMapper.toDomain(saved);
    }

    // Devuelve las planillas de una fecha
    @Override
    public List<PlanillaSanidad> findByFechaPlanilla(LocalDate fecha) {
        return repository.findByFechaPlanilla(fecha).stream()
                .map(PlanillaSanidadEntityMapper::toDomain)
                .toList();
    }

    // Devuelve las planillas entre dos fechas (inclusive)
    @Override
    public List<PlanillaSanidad> findByFechaPlanillaBetween(LocalDate inicio, LocalDate fin) {
        return repository.findByFechaPlanillaBetween(inicio, fin).stream()
                .map(PlanillaSanidadEntityMapper::toDomain)
                .toList();
    }
}