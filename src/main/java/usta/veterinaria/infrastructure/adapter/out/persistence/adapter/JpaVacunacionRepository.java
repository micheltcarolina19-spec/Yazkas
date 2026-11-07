package usta.veterinaria.infrastructure.adapter.out.persistence.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.port.out.VacunacionRepository;
import usta.veterinaria.domain.model.Vacunacion;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.MascotaEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.VacunacionEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.mapper.VacunacionEntityMapper;
import usta.veterinaria.infrastructure.adapter.out.persistence.repository.MascotaJpaRepository;
import usta.veterinaria.infrastructure.adapter.out.persistence.repository.VacunacionJpaRepository;

import java.util.List;

// Adaptador JPA del puerto VacunacionRepository: guarda/lee las vacunaciones en MySQL
@ApplicationScoped
public class JpaVacunacionRepository implements VacunacionRepository {

    @Inject
    VacunacionJpaRepository repository;

    @Inject
    MascotaJpaRepository mascotaRepository;

    // Guarda la vacunación (inserta o actualiza) resolviendo la mascota por su id
    @Override
    @Transactional
    public Vacunacion save(Vacunacion vacunacion) {
        if (vacunacion.getMascota() == null || vacunacion.getMascota().getId() == null) {
            throw new IllegalStateException("La vacunación debe tener una mascota");
        }

        MascotaEntity mascotaEntity = mascotaRepository.findByIdOptional(vacunacion.getMascota().getId())
                .orElseThrow(() -> new IllegalStateException(
                        "No existe la mascota con id " + vacunacion.getMascota().getId()));

        VacunacionEntity saved = repository.getEntityManager()
                .merge(VacunacionEntityMapper.toEntity(vacunacion, mascotaEntity));

        // La mascota es LAZY: se vuelve a leer con join fetch para que toDomain no falle
        return VacunacionEntityMapper.toDomain(saved);
    }

    // Devuelve las vacunaciones de una mascota
    @Override
    public List<Vacunacion> findByMascotaId(Long mascotaId) {
        return repository.findByMascotaId(mascotaId).stream()
                .map(VacunacionEntityMapper::toDomain)
                .toList();
    }

    // Devuelve las vacunaciones que tienen fecha de refuerzo
    @Override
    public List<Vacunacion> findConRefuerzo() {
        return repository.findConRefuerzo().stream()
                .map(VacunacionEntityMapper::toDomain)
                .toList();
    }
}