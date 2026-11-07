package usta.veterinaria.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.exception.ResourceNotFoundException;
import usta.veterinaria.application.port.in.GestionarProductosUseCase;
import usta.veterinaria.application.port.in.RegistrarVentaUseCase;
import usta.veterinaria.application.port.out.DetalleVentaRepository;
import usta.veterinaria.application.port.out.ProductoRepository;
import usta.veterinaria.application.port.out.VentaRepository;
import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.DetalleVenta;
import usta.veterinaria.domain.model.Producto;
import usta.veterinaria.domain.model.Venta;
import usta.veterinaria.domain.service.VentaService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class RegistrarVentaService implements RegistrarVentaUseCase {

    private final VentaService ventaService = new VentaService();

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final ProductoRepository productoRepository;
    private final GestionarProductosUseCase productos;

    @Inject
    public RegistrarVentaService(VentaRepository ventaRepository,
                                 DetalleVentaRepository detalleVentaRepository,
                                 ProductoRepository productoRepository,
                                 GestionarProductosUseCase productos) {
        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.productoRepository = productoRepository;
        this.productos = productos;
    }

    // Todo ocurre en una sola transacción: si algo falla no queda la venta guardada ni el stock descontado
    @Override
    @Transactional
    public Venta registrar(List<Long> productoIds, List<Integer> cantidades, LocalDate fechaVenta) {
        List<DetalleVenta> detalles = construirDetalles(productoIds, cantidades);

        // El dominio valida las líneas, calcula subtotales y total, y descuenta el stock (si no alcanza, queda en 0)
        Venta venta = ventaService.registrar(detalles, fechaVenta);

        Venta guardada = ventaRepository.save(venta);
        for (DetalleVenta detalle : detalles) {
            detalle.setVenta(guardada);
            productoRepository.save(detalle.getProducto());
        }
        detalleVentaRepository.saveAll(detalles);
        return guardada;
    }

    @Override
    public Venta buscarPorId(Long ventaId) {
        return ventaRepository.findById(Validaciones.requerirId(ventaId, "id de la venta"))
                .orElseThrow(() -> new ResourceNotFoundException("No existe la venta con id " + ventaId));
    }

    @Override
    public List<DetalleVenta> listarDetalles(Long ventaId) {
        buscarPorId(ventaId);
        return detalleVentaRepository.findByVentaId(ventaId);
    }

    @Override
    public List<Venta> listarPorRango(LocalDate inicio, LocalDate fin) {
        Validaciones.rangoFechas(inicio, fin);
        return ventaRepository.findByFechaVentaBetween(inicio, fin);
    }

    // Une cada producto con su cantidad. Si no llegan listas se deja la venta vacía y el dominio lo rechaza.
    private List<DetalleVenta> construirDetalles(List<Long> productoIds, List<Integer> cantidades) {
        List<DetalleVenta> detalles = new ArrayList<>();
        if (productoIds == null || cantidades == null) {
            return detalles;
        }
        if (productoIds.size() != cantidades.size()) {
            throw new BusinessRuleException("Cada producto de la venta debe tener su cantidad");
        }
        for (int i = 0; i < productoIds.size(); i++) {
            Producto producto = productos.buscarPorId(productoIds.get(i));
            detalles.add(DetalleVenta.builder()
                    .producto(producto)
                    .cantidad(cantidades.get(i))
                    .build());
        }
        return detalles;
    }
}
