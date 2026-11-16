package usta.veterinaria.infrastructure.adapter.out.persistence.adapter.admin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.port.out.admin.BitacoraServicioRepository;
import usta.veterinaria.domain.model.admin.BitacoraServicio;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.admin.BitacoraServicioEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.mapper.admin.BitacoraServicioEntityMapper;
import usta.veterinaria.infrastructure.adapter.out.persistence.repository.admin.BitacoraServicioJpaRepository;

import java.util.Collection;
import java.util.List;

// Adaptador JPA del puerto BitacoraServicioRepository: guarda/lee los servicios prestados en una cita en MySQL
@ApplicationScoped
public class JpaBitacoraServicioRepository implements BitacoraServicioRepository {

    // Trae la cita, la vacuna y la desparasitación en la misma consulta (evita lecturas perezosas)
    private static final String SELECT_CON_RELACIONES =
            "select distinct b from BitacoraServicioEntity b "
                    + "join fetch b.agendamientoCita a "
                    + "left join fetch b.vacunacion "
                    + "left join fetch b.desparasitacion ";

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
        return repository.list(SELECT_CON_RELACIONES + "where a.id = ?1", agendamientoId).stream()
                .map(BitacoraServicioEntityMapper::toDomain)
                .toList();
    }

    // Devuelve los servicios de varios agendamientos en una sola consulta (para el calendario)
    @Override
    public List<BitacoraServicio> findByAgendamientoCitaIds(Collection<Long> agendamientoIds) {
        if (agendamientoIds == null || agendamientoIds.isEmpty()) {
            return List.of();
        }
        return repository.list(SELECT_CON_RELACIONES + "where a.id in ?1", List.copyOf(agendamientoIds)).stream()
                .map(BitacoraServicioEntityMapper::toDomain)
                .toList();
    }
}
