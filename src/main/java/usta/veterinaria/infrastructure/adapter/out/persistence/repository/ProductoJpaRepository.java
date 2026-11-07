package usta.veterinaria.infrastructure.adapter.out.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.ProductoEntity;

import java.util.List;

// Repositorio Panache de productos
@ApplicationScoped
public class ProductoJpaRepository implements PanacheRepository<ProductoEntity> {

    // Todos los productos, ordenados por nombre
    public List<ProductoEntity> findAllOrdenados() {
        return list("order by nombre");
    }
}