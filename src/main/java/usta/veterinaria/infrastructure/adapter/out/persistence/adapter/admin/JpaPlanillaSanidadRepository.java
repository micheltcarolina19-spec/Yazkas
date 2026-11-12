package usta.veterinaria.infrastructure.adapter.out.persistence.adapter.admin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.port.out.admin.PlanillaSanidadRepository;
import usta.veterinaria.domain.model.admin.PlanillaSanidad;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.admin.PlanillaSanidadEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.mapper.admin.PlanillaSanidadEntityMapper;
import usta.veterinaria.infrastructure.adapter.out.persistence.repository.admin.PlanillaSanidadJpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Adaptador JPA del puerto PlanillaSanidadRepository: guarda/lee las planillas en MySQL
@ApplicationScoped
public class JpaPlanillaSanidadRepository implements PlanillaSanidadRepository {

    @Inject
    PlanillaSanidadJpaRepository repository;

    // Guarda la planilla: sin id inserta; con id actualiza la fila existente (merge)
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

    // Busca una planilla por su id
    @Override
    public Optional<PlanillaSanidad> findById(Long id) {
        return repository.findByIdOptional(id).map(PlanillaSanidadEntityMapper::toDomain);
    }

    // Elimina la planilla con ese id (si no existe no hace nada)
    @Override
    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}