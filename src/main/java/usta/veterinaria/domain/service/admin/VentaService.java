package usta.veterinaria.domain.service.admin;

import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.admin.DetalleVenta;
import usta.veterinaria.domain.model.admin.Producto;
import usta.veterinaria.domain.model.admin.Venta;
import usta.veterinaria.domain.service.MonedaColombiana;
import usta.veterinaria.domain.service.RelojColombia;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

// Servicio que registra las ventas de productos que el administrador ya hizo de forma presencial.
// subtotal = precioVenta x cantidad ; total = suma de los subtotales (en pesos colombianos).
public class VentaService {

    private final Clock clock;

    public VentaService() {
        this(RelojColombia.reloj());
    }

    public VentaService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "El reloj es obligatorio");
    }

    // Registra la venta, calcula subtotales y total, y descuenta el stock de cada producto.
    // fechaVenta puede ser null (se toma hoy) o un día anterior si se registra después.
    public Venta registrar(List<DetalleVenta> detalles, LocalDate fechaVenta) {
        if (detalles == null || detalles.isEmpty()) {
            throw new BusinessRuleException("La venta debe tener al menos un producto");
        }
        LocalDate hoy = LocalDate.now(clock);
        LocalDate fecha = fechaVenta == null ? hoy : fechaVenta;
        if (fecha.isAfter(hoy)) {
            throw new BusinessRuleException("No se puede registrar una venta con fecha futura");
        }

        // Primero se valida todo para no dejar la venta a medias
        Set<Object> productosVistos = new HashSet<>();
        for (DetalleVenta detalle : detalles) {
            validarDetalle(detalle);
            Producto producto = detalle.getProducto();
            Object clave = producto.getId() != null ? producto.getId() : producto;
            if (!productosVistos.add(clave)) {
                throw new BusinessRuleException("El producto " + producto.getNombre()
                        + " está repetido en la venta; registre una sola línea con la cantidad total");
            }
        }

        Venta venta = Venta.builder()
                .fechaVenta(fecha)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (DetalleVenta detalle : detalles) {
            Producto producto = detalle.getProducto();
            BigDecimal subtotal = calcularSubtotal(producto, detalle.getCantidad());
            detalle.setSubtotal(subtotal);
            detalle.setVenta(venta);
            total = total.add(subtotal);
            descontarStock(producto, detalle.getCantidad());
        }

        venta.setTotalVenta(total);
        return venta;
    }

    // subtotal = precioVenta x cantidad, redondeado a pesos enteros
    public BigDecimal calcularSubtotal(Producto producto, int cantidad) {
        BigDecimal precioVenta = MonedaColombiana.validarMontoPositivo(
                producto.getPrecioVenta(), "precio de venta de " + producto.getNombre());
        return MonedaColombiana.normalizar(precioVenta.multiply(BigDecimal.valueOf(cantidad)));
    }

    // Valida que la línea tenga producto y una cantidad mayor a cero
    private void validarDetalle(DetalleVenta detalle) {
        if (detalle == null || detalle.getProducto() == null) {
            throw new BusinessRuleException("Cada línea de la venta debe tener un producto");
        }
        if (detalle.getCantidad() == null || detalle.getCantidad() <= 0) {
            throw new BusinessRuleException("La cantidad de " + detalle.getProducto().getNombre()
                    + " debe ser mayor a cero");
        }
        if (detalle.getProducto().getPrecioVenta() == null) {
            throw new BusinessRuleException("El producto " + detalle.getProducto().getNombre()
                    + " no tiene precio de venta registrado");
        }
    }

    // La venta ya ocurrió en persona, así que no se bloquea: si el stock no alcanza queda en 0
    private void descontarStock(Producto producto, int cantidad) {
        int stockActual = producto.getStock() == null ? 0 : producto.getStock();
        producto.setStock(Math.max(0, stockActual - cantidad));
    }
}
