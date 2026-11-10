package usta.veterinaria.application.usecase.admin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.exception.ResourceNotFoundException;
import usta.veterinaria.application.port.in.admin.GestionarGastosUseCase;
import usta.veterinaria.application.port.out.admin.GastoRepository;
import usta.veterinaria.application.usecase.Validaciones;
import usta.veterinaria.domain.model.admin.CategoriaGasto;
import usta.veterinaria.domain.model.admin.Gasto;
import usta.veterinaria.domain.service.admin.GastoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class GestionarGastosService implements GestionarGastosUseCase {

    private final GastoService gastoService = new GastoService();

    private final GastoRepository gastoRepository;

    @Inject
    public GestionarGastosService(GastoRepository gastoRepository) {
        this.gastoRepository = gastoRepository;
    }

    @Override
    @Transactional
    public Gasto registrar(String nombre, BigDecimal monto, LocalDate fechaGasto, CategoriaGasto categoria) {
        Gasto gasto = construir(nombre, monto, fechaGasto, categoria);
        return gastoRepository.save(gastoService.registrar(gasto));
    }

    @Override
    @Transactional
    public Gasto actualizar(Long id, String nombre, BigDecimal monto, LocalDate fechaGasto,
                            CategoriaGasto categoria) {
        Gasto existente = buscarPorId(id);

        Gasto actualizado = construir(nombre, monto, fechaGasto, categoria);
        actualizado.setId(existente.getId());

        return gastoRepository.save(gastoService.registrar(actualizado));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Gasto gasto = buscarPorId(id);
        gastoRepository.deleteById(gasto.getId());
    }

    @Override
    public Gasto buscarPorId(Long id) {
        return gastoRepository.findById(Validaciones.requerirId(id, "id del gasto"))
                .orElseThrow(() -> new ResourceNotFoundException("No existe el gasto con id " + id));
    }

    @Override
    public List<Gasto> listar() {
        return gastoRepository.findAll();
    }

    @Override
    public List<Gasto> listarPorRango(LocalDate inicio, LocalDate fin) {
        Validaciones.rangoFechas(inicio, fin);
        return gastoRepository.findByFechaGastoBetween(inicio, fin);
    }

    private Gasto construir(String nombre, BigDecimal monto, LocalDate fechaGasto, CategoriaGasto categoria) {
        return Gasto.builder()
                .nombre(nombre)
                .monto(monto)
                .fechaGasto(fechaGasto)
                .categoria(categoria)
                .build();
    }
}
