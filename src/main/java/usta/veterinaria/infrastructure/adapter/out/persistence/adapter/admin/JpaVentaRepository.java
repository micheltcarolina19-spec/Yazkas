package usta.veterinaria.infrastructure.adapter.out.persistence.adapter.admin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.port.out.admin.VentaRepository;
import usta.veterinaria.domain.model.admin.Venta;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.admin.VentaEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.mapper.admin.VentaEntityMapper;
import usta.veterinaria.infrastructure.adapter.out.persistence.repository.admin.VentaJpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Adaptador JPA del puerto VentaRepository: guarda/lee las ventas en MySQL
@ApplicationScoped
public class JpaVentaRepository implements VentaRepository {

    @Inject
    VentaJpaRepository repository;

    // Guarda la venta (inserta o actualiza)
    @Override
    @Transactional
    public Venta save(Venta venta) {
        VentaEntity saved = repository.getEntityManager()
                .merge(VentaEntityMapper.toEntity(venta));
        return VentaEntityMapper.toDomain(saved);
    }

    // Busca una venta por id
    @Override
    public Optional<Venta> findById(Long id) {
        return repository.findByIdOptional(id).map(VentaEntityMapper::toDomain);
    }

    // Devuelve las ventas entre dos fechas (inclusive)
    @Override
    public List<Venta> findByFechaVentaBetween(LocalDate inicio, LocalDate fin) {
        return repository.findByFechaVentaBetween(inicio, fin).stream()
                .map(VentaEntityMapper::toDomain)
                .toList();
    }
}