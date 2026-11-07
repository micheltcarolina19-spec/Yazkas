package usta.veterinaria.infrastructure.adapter.out.persistence.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.port.out.HistoriaClinicaRepository;
import usta.veterinaria.domain.model.HistoriaClinica;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.HistorialClinicaEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.mapper.HistorialClinicaEntityMapper;
import usta.veterinaria.infrastructure.adapter.out.persistence.repository.HistorialClinicaJpaRepository;

import java.util.Optional;

// Adaptador JPA del puerto HistoriaClinicaRepository: guarda/lee la historia clínica de cada mascota en MySQL
@ApplicationScoped
public class JpaHistorialClinicaRepository implements HistoriaClinicaRepository {

    @Inject
    HistorialClinicaJpaRepository repository;

    // Guarda la historia clínica (inserta o actualiza) y la devuelve con su id
    @Override
    @Transactional
    public HistoriaClinica save(HistoriaClinica historiaClinica) {
        HistorialClinicaEntity saved = repository.getEntityManager()
                .merge(HistorialClinicaEntityMapper.toEntity(historiaClinica));
        return HistorialClinicaEntityMapper.toDomain(saved);
    }

    // Busca la historia clínica de una mascota (como máximo existe una)
    @Override
    public Optional<HistoriaClinica> findByMascotaId(Long mascotaId) {
        return repository.findByMascotaId(mascotaId).map(HistorialClinicaEntityMapper::toDomain);
    }
}