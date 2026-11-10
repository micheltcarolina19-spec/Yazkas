package usta.veterinaria.infrastructure.adapter.out.persistence.repository.admin;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.admin.DetalleVentaEntity;

import java.time.LocalDate;
import java.util.List;

// Repositorio Panache de detalles de venta: aquí viven las consultas con nombre.
// Todas traen la venta y el producto cargados, porque el mapper los convierte al dominio
// y los reportes usan la fecha de la venta y la categoría y el precio base del producto.
@ApplicationScoped
public class DetalleVentaJpaRepository implements PanacheRepository<DetalleVentaEntity> {

    private static final String SELECT_CON_RELACIONES =
            "select d from DetalleVentaEntity d join fetch d.venta v join fetch d.producto p ";

    // Líneas de una venta
    public List<DetalleVentaEntity> findByVentaId(Long ventaId) {
        return list(SELECT_CON_RELACIONES + "where v.id = ?1", ventaId);
    }

    // Líneas de las ventas hechas entre inicio y fin (ambas inclusive)
    public List<DetalleVentaEntity> findByVentaFechaVentaBetween(LocalDate inicio, LocalDate fin) {
        return list(SELECT_CON_RELACIONES + "where v.fechaVenta between ?1 and ?2", inicio, fin);
    }
}