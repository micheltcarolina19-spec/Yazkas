package usta.veterinaria.infrastructure.adapter.out.persistence.adapter.usuario;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.port.out.usuario.AgendamientoCitaRepository;
import usta.veterinaria.domain.model.usuario.AgendamientoCita;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.usuario.AgendamientoCitaEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.mapper.usuario.AgendamientoCitaEntityMapper;
import usta.veterinaria.infrastructure.adapter.out.persistence.repository.usuario.AgendamientoJpaRepository;

import java.util.Optional;

// Adaptador JPA del puerto AgendamientoCitaRepository: guarda/lee agendamientos de citas en MySQL
@ApplicationScoped
public class JpaAgendamientoRepository implements AgendamientoCitaRepository {

    @Inject
    AgendamientoJpaRepository repository;

    // Guarda el agendamiento (inserta o actualiza) y lo devuelve con su id
    @Override
    @Transactional
    public AgendamientoCita save(AgendamientoCita agendamientoCita) {
        AgendamientoCitaEntity saved = repository.getEntityManager()
                .merge(AgendamientoCitaEntityMapper.toEntity(agendamientoCita));
        return AgendamientoCitaEntityMapper.toDomain(saved);
    }

    // Busca un agendamiento por id
    @Override
    public Optional<AgendamientoCita> findById(Long id) {
        return repository.findByIdOptional(id).map(AgendamientoCitaEntityMapper::toDomain);
    }
}