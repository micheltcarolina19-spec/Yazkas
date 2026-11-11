package usta.veterinaria.infrastructure.adapter.out.persistence.repository.mascota;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.mascota.DesparasitacionEntity;
@ApplicationScoped
public class DesparasitacionJpaRepository implements PanacheRepository<DesparasitacionEntity> {
}
