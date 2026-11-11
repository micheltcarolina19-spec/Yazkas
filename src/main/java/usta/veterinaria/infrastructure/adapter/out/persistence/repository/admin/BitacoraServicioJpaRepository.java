package usta.veterinaria.infrastructure.adapter.out.persistence.repository.admin;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.admin.BitacoraServicioEntity;

@ApplicationScoped
public class BitacoraServicioJpaRepository implements PanacheRepository<BitacoraServicioEntity> {
}
