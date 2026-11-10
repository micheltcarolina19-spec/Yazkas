package usta.veterinaria.infrastructure.adapter.out.persistence.adapter.mascota;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.port.out.mascota.DesparasitacionRepository;
import usta.veterinaria.domain.model.mascota.Desparasitacion;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.mascota.DesparasitacionEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.mapper.mascota.DesparasitacionEntityMapper;
import usta.veterinaria.infrastructure.adapter.out.persistence.repository.mascota.DesparasitacionJpaRepository;

import java.util.List;

// Adaptador JPA del puerto DesparasitacionRepository: guarda/lee las desparasitaciones de las mascotas en MySQL
@ApplicationScoped
public class JpaDesparasitacionRepository implements DesparasitacionRepository {

    @Inject
    DesparasitacionJpaRepository repository;

    // Guarda la desparasitación (inserta o actualiza) y la devuelve con su id
    @Override
    @Transactional
    public Desparasitacion save(Desparasitacion desparasitacion) {
        DesparasitacionEntity saved = repository.getEntityManager()
                .merge(DesparasitacionEntityMapper.toEntity(desparasitacion));
        return DesparasitacionEntityMapper.toDomain(saved);
    }

    // Devuelve las desparasitaciones de una mascota
    @Override
    public List<Desparasitacion> findByMascotaId(Long mascotaId) {
        return repository.list("mascota.id", mascotaId).stream()
                .map(DesparasitacionEntityMapper::toDomain)
                .toList();
    }
}