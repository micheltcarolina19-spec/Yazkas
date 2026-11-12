package usta.veterinaria.application.usecase.admin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.exception.ResourceNotFoundException;
import usta.veterinaria.application.port.in.admin.GestionarProductosUseCase;
import usta.veterinaria.application.port.in.admin.RegistrarVentaUseCase;
import usta.veterinaria.application.port.out.admin.DetalleVentaRepository;
import usta.veterinaria.application.port.out.admin.ProductoRepository;
import usta.veterinaria.application.port.out.admin.VentaRepository;
import usta.veterinaria.application.usecase.Validaciones;
import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.admin.DetalleVenta;
import usta.veterinaria.domain.model.admin.Producto;
import usta.veterinaria.domain.model.admin.Venta;
import usta.veterinaria.domain.service.admin.VentaService;

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

    // Arma el ticket del punto de venta SIN guardar nada: toma el precio y el stock reales de cada producto
    // y calcula el subtotal con la misma regla del dominio. Lo que no se puede vender se ajusta:
    //  - el producto que ya no existe, no tiene precio o está agotado no entra en el ticket
    //  - si piden más unidades de las que hay, se deja solo el stock disponible
    @Override
    public List<DetalleVenta> armarTicket(List<Long> productoIds, List<Integer> cantidades) {
        List<DetalleVenta> ticket = new ArrayList<>();
        if (productoIds == null || cantidades == null) {
            return ticket;
        }
        if (productoIds.size() != cantidades.size()) {
            throw new BusinessRuleException("Cada producto del ticket debe tener su cantidad");
        }
        for (int i = 0; i < productoIds.size(); i++) {
            Long id = productoIds.get(i);
            Integer cantidad = cantidades.get(i);
            if (id == null || cantidad == null || cantidad <= 0) {
                continue;
            }
            Producto producto = productoRepository.findById(id).orElse(null);
            if (producto == null || producto.getPrecioVenta() == null) {
                continue;
            }
            int stock = producto.getStock() == null ? 0 : producto.getStock();
            int vendible = Math.min(cantidad, stock);
            if (vendible <= 0) {
                continue;
            }
            DetalleVenta detalle = DetalleVenta.builder()
                    .producto(producto)
                    .cantidad(vendible)
                    .build();
            detalle.setSubtotal(ventaService.calcularSubtotal(producto, vendible));
            ticket.add(detalle);
        }
        return ticket;
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