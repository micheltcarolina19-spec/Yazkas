package usta.veterinaria.infrastructure.adapter.out.persistence.adapter.admin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.port.out.admin.ProductoRepository;
import usta.veterinaria.domain.model.admin.Producto;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.admin.ProductoEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.mapper.admin.ProductoEntityMapper;
import usta.veterinaria.infrastructure.adapter.out.persistence.repository.admin.ProductoJpaRepository;

import java.util.List;
import java.util.Optional;

// Adaptador JPA del puerto ProductoRepository: guarda/lee los productos en MySQL
@ApplicationScoped
public class JpaProductoRepository implements ProductoRepository {

    @Inject
    ProductoJpaRepository repository;

    // Guarda el producto (inserta o actualiza)
    @Override
    @Transactional
    public Producto save(Producto producto) {
        ProductoEntity saved = repository.getEntityManager()
                .merge(ProductoEntityMapper.toEntity(producto));
        return ProductoEntityMapper.toDomain(saved);
    }

    // Busca un producto por id
    @Override
    public Optional<Producto> findById(Long id) {
        return repository.findByIdOptional(id).map(ProductoEntityMapper::toDomain);
    }

    // Devuelve todos los productos
    @Override
    public List<Producto> findAll() {
        return repository.findAllOrdenados().stream()
                .map(ProductoEntityMapper::toDomain)
                .toList();
    }

    // Elimina el producto por id
    @Override
    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}