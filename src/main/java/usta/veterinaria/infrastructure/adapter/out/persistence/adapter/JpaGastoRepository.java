package usta.veterinaria.infrastructure.adapter.out.persistence.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.port.out.GastoRepository;
import usta.veterinaria.domain.model.Gasto;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.GastoEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.mapper.GastoEntityMapper;
import usta.veterinaria.infrastructure.adapter.out.persistence.repository.GastoJpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Adaptador JPA del puerto GastoRepository: guarda/lee los gastos del negocio en MySQL
@ApplicationScoped
public class JpaGastoRepository implements GastoRepository {

    @Inject
    GastoJpaRepository repository;

    // Guarda el gasto (inserta o actualiza) y lo devuelve con su id
    @Override
    @Transactional
    public Gasto save(Gasto gasto) {
        GastoEntity saved = repository.getEntityManager().merge(GastoEntityMapper.toEntity(gasto));
        return GastoEntityMapper.toDomain(saved);
    }

    // Busca un gasto por id
    @Override
    public Optional<Gasto> findById(Long id) {
        return repository.findByIdOptional(id).map(GastoEntityMapper::toDomain);
    }

    // Devuelve todos los gastos
    @Override
    public List<Gasto> findAll() {
        return repository.listAll().stream().map(GastoEntityMapper::toDomain).toList();
    }

    // Devuelve los gastos con fecha entre inicio y fin (ambas inclusive)
    @Override
    public List<Gasto> findByFechaGastoBetween(LocalDate inicio, LocalDate fin) {
        return repository.findByFechaGastoBetween(inicio, fin).stream()
                .map(GastoEntityMapper::toDomain)
                .toList();
    }

    // Elimina el gasto por id
    @Override
    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}