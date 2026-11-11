package usta.veterinaria.infrastructure.adapter.out.persistence.repository.usuario;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.usuario.AgendamientoCitaEntity;
@ApplicationScoped

public class AgendamientoJpaRepository implements PanacheRepository<AgendamientoCitaEntity>{
}
