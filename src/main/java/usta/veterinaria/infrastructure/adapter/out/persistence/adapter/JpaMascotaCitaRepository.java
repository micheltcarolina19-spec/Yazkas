package usta.veterinaria.infrastructure.adapter.out.persistence.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.port.out.MascotaCitaRepository;
import usta.veterinaria.domain.model.MascotaCita;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.MascotaCitaEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.mapper.MascotaCitaEntityMapper;
import usta.veterinaria.infrastructure.adapter.out.persistence.repository.MascotaCitaJpaRepository;

import java.time.LocalDate;
import java.util.List;

// Adaptador JPA del puerto MascotaCitaRepository: guarda/lee las citas de cada mascota en MySQL
@ApplicationScoped
public class JpaMascotaCitaRepository implements MascotaCitaRepository {

    @Inject
    MascotaCitaJpaRepository repository;

    // Guarda las citas del agendamiento (inserta o actualiza) y las devuelve
    @Override
    @Transactional
    public List<MascotaCita> saveAll(List<MascotaCita> citas) {
        EntityManager em = repository.getEntityManager();
        return citas.stream()
                .map(cita -> {
                    MascotaCitaEntity saved = em.merge(MascotaCitaEntityMapper.toEntity(cita));
                    return MascotaCitaEntityMapper.toDomain(saved);
                })
                .toList();
    }

    // Devuelve las citas de un agendamiento
    @Override
    public List<MascotaCita> findByAgendamientoCitaId(Long agendamientoId) {
        return repository.findByAgendamientoCitaId(agendamientoId).stream()
                .map(MascotaCitaEntityMapper::toDomain)
                .toList();
    }

    // Devuelve las citas de una mascota, de la más reciente a la más antigua
    @Override
    public List<MascotaCita> findByMascotaId(Long mascotaId) {
        return repository.findByMascotaId(mascotaId).stream()
                .map(MascotaCitaEntityMapper::toDomain)
                .toList();
    }

    // Devuelve las citas con fecha entre inicio y fin (ambas inclusive)
    @Override
    public List<MascotaCita> findByFechaAgendamientoBetween(LocalDate inicio, LocalDate fin) {
        return repository.findByFechaAgendamientoBetween(inicio, fin).stream()
                .map(MascotaCitaEntityMapper::toDomain)
                .toList();
    }

    // Devuelve las citas con fecha igual o posterior a la indicada (para validar cupos hacia adelante)
    @Override
    public List<MascotaCita> findByFechaAgendamientoDesde(LocalDate desde) {
        return repository.findByFechaAgendamientoDesde(desde).stream()
                .map(MascotaCitaEntityMapper::toDomain)
                .toList();
    }
}