package usta.veterinaria.infrastructure.adapter.out.persistence.adapter.admin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.port.out.admin.BitacoraServicioRepository;
import usta.veterinaria.domain.model.admin.BitacoraServicio;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.admin.BitacoraServicioEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.mapper.admin.BitacoraServicioEntityMapper;
import usta.veterinaria.infrastructure.adapter.out.persistence.repository.admin.BitacoraServicioJpaRepository;

import java.util.List;

// Adaptador JPA del puerto BitacoraServicioRepository: guarda/lee los servicios prestados en una cita en MySQL
@ApplicationScoped
public class JpaBitacoraServicioRepository implements BitacoraServicioRepository {

    @Inject
    BitacoraServicioJpaRepository repository;

    // Guarda el servicio de la bitácora (inserta o actualiza) y lo devuelve con su id
    @Override
    @Transactional
    public BitacoraServicio save(BitacoraServicio servicio) {
        BitacoraServicioEntity saved = repository.getEntityManager()
                .merge(BitacoraServicioEntityMapper.toEntity(servicio));
        return BitacoraServicioEntityMapper.toDomain(saved);
    }

    // Devuelve los servicios registrados para un agendamiento
    @Override
    public List<BitacoraServicio> findByAgendamientoCitaId(Long agendamientoId) {
        return repository.list("agendamientoCita.id", agendamientoId).stream()
                .map(BitacoraServicioEntityMapper::toDomain)
                .toList();
    }
}