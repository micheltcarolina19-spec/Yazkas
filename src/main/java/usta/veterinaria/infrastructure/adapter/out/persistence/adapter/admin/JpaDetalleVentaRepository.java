package usta.veterinaria.infrastructure.adapter.out.persistence.adapter.admin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.port.out.admin.DetalleVentaRepository;
import usta.veterinaria.domain.model.admin.DetalleVenta;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.admin.DetalleVentaEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.mapper.admin.DetalleVentaEntityMapper;
import usta.veterinaria.infrastructure.adapter.out.persistence.repository.admin.DetalleVentaJpaRepository;

import java.time.LocalDate;
import java.util.List;

// Adaptador JPA del puerto DetalleVentaRepository: guarda/lee las líneas de cada venta en MySQL
@ApplicationScoped
public class JpaDetalleVentaRepository implements DetalleVentaRepository {

    @Inject
    DetalleVentaJpaRepository repository;

    // Guarda todas las líneas de la venta (inserta o actualiza) y las devuelve
    @Override
    @Transactional
    public List<DetalleVenta> saveAll(List<DetalleVenta> detalles) {
        EntityManager em = repository.getEntityManager();
        return detalles.stream()
                .map(detalle -> {
                    DetalleVentaEntity saved = em.merge(DetalleVentaEntityMapper.toEntity(detalle));
                    return DetalleVentaEntityMapper.toDomain(saved);
                })
                .toList();
    }

    // Devuelve las líneas de una venta
    @Override
    public List<DetalleVenta> findByVentaId(Long ventaId) {
        return repository.findByVentaId(ventaId).stream().map(DetalleVentaEntityMapper::toDomain).toList();
    }

    // Devuelve las líneas de las ventas hechas entre inicio y fin (ambas inclusive), para los reportes
    @Override
    public List<DetalleVenta> findByVentaFechaVentaBetween(LocalDate inicio, LocalDate fin) {
        return repository.findByVentaFechaVentaBetween(inicio, fin).stream()
                .map(DetalleVentaEntityMapper::toDomain)
                .toList();
    }
}