package usta.veterinaria.domain.service.admin;

import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.usuario.AgendamientoCita;
import usta.veterinaria.domain.model.admin.CategoriaGasto;
import usta.veterinaria.domain.model.admin.CategoriaProducto;
import usta.veterinaria.domain.model.admin.DetalleVenta;
import usta.veterinaria.domain.model.usuario.EstadoCita;
import usta.veterinaria.domain.model.admin.Gasto;
import usta.veterinaria.domain.model.usuario.MascotaCita;
import usta.veterinaria.domain.model.admin.Producto;
import usta.veterinaria.domain.model.admin.Venta;
import usta.veterinaria.domain.service.MonedaColombiana;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Servicio que calcula los valores del panel de finanzas en un rango de fechas.
// Todo se maneja en pesos colombianos (COP) y sin centavos.
//   Ingresos = ventas de productos + citas completadas
//   Utilidad = ingresos - gastos
public class FinanzasService {

    // Tope de tramos por consulta para que un rango enorme no sature el servidor
    private static final int MAX_TRAMOS_SERIE = 400;

    // Total vendido en productos dentro del rango
    public BigDecimal ingresosPorVentas(List<Venta> ventas, LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        BigDecimal total = BigDecimal.ZERO;
        if (ventas != null) {
            for (Venta venta : ventas) {
                if (venta != null && enRango(venta.getFechaVenta(), inicio, fin)) {
                    total = total.add(MonedaColombiana.normalizar(venta.getTotalVenta()));
                }
            }
        }
        return total;
    }

    // Total cobrado por citas COMPLETADAS dentro del rango.
    // Se recibe MascotaCita porque ahí está la fecha; cada agendamiento se suma una sola vez
    // aunque tenga varias mascotas.
    public BigDecimal ingresosPorCitas(List<MascotaCita> citas, LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        Map<Object, AgendamientoCita> agendamientos = new LinkedHashMap<>();
        if (citas != null) {
            for (MascotaCita cita : citas) {
                if (cita == null || cita.getAgendamientoCita() == null) {
                    continue;
                }
                AgendamientoCita agendamiento = cita.getAgendamientoCita();
                if (agendamiento.getEstado() == EstadoCita.COMPLETADA
                        && enRango(cita.getFechaAgendamiento(), inicio, fin)) {
                    Object clave = agendamiento.getId() != null ? agendamiento.getId() : agendamiento;
                    agendamientos.putIfAbsent(clave, agendamiento);
                }
            }
        }
        BigDecimal total = BigDecimal.ZERO;
        for (AgendamientoCita agendamiento : agendamientos.values()) {
            total = total.add(MonedaColombiana.normalizar(agendamiento.getPrecioTotal()));
        }
        return total;
    }

    // Total de gastos dentro del rango
    public BigDecimal totalGastos(List<Gasto> gastos, LocalDate inicio, LocalDate fin) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal valor : gastosPorCategoria(gastos, inicio, fin).values()) {
            total = total.add(valor);
        }
        return total;
    }

    // Ingresos totales del negocio: productos + citas
    public BigDecimal ingresosTotales(List<Venta> ventas, List<MascotaCita> citas, LocalDate inicio, LocalDate fin) {
        return ingresosPorVentas(ventas, inicio, fin).add(ingresosPorCitas(citas, inicio, fin));
    }

    // Utilidad del periodo: ingresos - gastos (puede ser negativa si hubo pérdida)
    public BigDecimal utilidad(List<Venta> ventas, List<MascotaCita> citas, List<Gasto> gastos,
                               LocalDate inicio, LocalDate fin) {
        return ingresosTotales(ventas, citas, inicio, fin).subtract(totalGastos(gastos, inicio, fin));
    }

    // Gastos agrupados por categoría (todas las categorías aparecen, aunque estén en $0)
    public Map<CategoriaGasto, BigDecimal> gastosPorCategoria(List<Gasto> gastos, LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        Map<CategoriaGasto, BigDecimal> resultado = new EnumMap<>(CategoriaGasto.class);
        for (CategoriaGasto categoria : CategoriaGasto.values()) {
            resultado.put(categoria, BigDecimal.ZERO);
        }
        if (gastos != null) {
            for (Gasto gasto : gastos) {
                if (gasto != null && gasto.getCategoria() != null && enRango(gasto.getFechaGasto(), inicio, fin)) {
                    resultado.merge(gasto.getCategoria(), MonedaColombiana.normalizar(gasto.getMonto()), BigDecimal::add);
                }
            }
        }
        return resultado;
    }

    // Ventas agrupadas por categoría de producto (todas las categorías aparecen, aunque estén en $0)
    public Map<CategoriaProducto, BigDecimal> ventasPorCategoriaProducto(List<DetalleVenta> detalles,
                                                                         LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        Map<CategoriaProducto, BigDecimal> resultado = new EnumMap<>(CategoriaProducto.class);
        for (CategoriaProducto categoria : CategoriaProducto.values()) {
            resultado.put(categoria, BigDecimal.ZERO);
        }
        if (detalles != null) {
            for (DetalleVenta detalle : detalles) {
                if (detalleEnRango(detalle, inicio, fin) && detalle.getProducto().getCategoria() != null) {
                    resultado.merge(detalle.getProducto().getCategoria(),
                            MonedaColombiana.normalizar(detalle.getSubtotal()), BigDecimal::add);
                }
            }
        }
        return resultado;
    }

    // Ganancia aproximada de los productos vendidos: subtotal - (precioBase x cantidad).
    // OJO: usa el precioBase ACTUAL del producto, porque DetalleVenta no guarda el costo del momento.
    public BigDecimal gananciaProductos(List<DetalleVenta> detalles, LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        BigDecimal ganancia = BigDecimal.ZERO;
        if (detalles != null) {
            for (DetalleVenta detalle : detalles) {
                if (!detalleEnRango(detalle, inicio, fin)) {
                    continue;
                }
                Producto producto = detalle.getProducto();
                int cantidad = detalle.getCantidad() == null ? 0 : detalle.getCantidad();
                BigDecimal costo = MonedaColombiana.normalizar(producto.getPrecioBase())
                        .multiply(BigDecimal.valueOf(cantidad));
                ganancia = ganancia.add(MonedaColombiana.normalizar(detalle.getSubtotal()).subtract(costo));
            }
        }
        return ganancia;
    }

    // Resumen de ventas de un producto en el periodo (resultado del ranking de más vendidos)
    public record ProductoVendido(Long productoId, String nombre, String fotoProducto,
                                  CategoriaProducto categoria, long unidadesVendidas,
                                  BigDecimal totalVendido) {
    }

    // Productos más vendidos del rango, ordenados por unidades vendidas (de mayor a menor).
    // Si hay empate en unidades gana el que más dinero generó, y luego el orden alfabético.
    public List<ProductoVendido> productosMasVendidos(List<DetalleVenta> detalles, LocalDate inicio,
                                                      LocalDate fin, int limite) {
        validarRango(inicio, fin);
        if (limite <= 0) {
            throw new BusinessRuleException("El límite de productos debe ser mayor a cero");
        }

        // Se agrupa por producto (id); si el producto no tiene id se agrupa por la instancia
        Map<Object, Acumulado> acumulados = new LinkedHashMap<>();
        if (detalles != null) {
            for (DetalleVenta detalle : detalles) {
                if (!detalleEnRango(detalle, inicio, fin)) {
                    continue;
                }
                Producto producto = detalle.getProducto();
                Object clave = producto.getId() != null ? producto.getId() : producto;
                Acumulado acumulado = acumulados.computeIfAbsent(clave, k -> new Acumulado(producto));
                acumulado.unidades += detalle.getCantidad() == null ? 0 : detalle.getCantidad();
                acumulado.total = acumulado.total.add(MonedaColombiana.normalizar(detalle.getSubtotal()));
            }
        }

        return acumulados.values().stream()
                .filter(a -> a.unidades > 0)
                .map(a -> new ProductoVendido(a.producto.getId(), a.producto.getNombre(),
                        a.producto.getFotoProducto(), a.producto.getCategoria(), a.unidades, a.total))
                .sorted(Comparator.comparingLong(ProductoVendido::unidadesVendidas).reversed()
                        .thenComparing(ProductoVendido::totalVendido, Comparator.reverseOrder())
                        .thenComparing(ProductoVendido::nombre, Comparator.nullsLast(String::compareToIgnoreCase)))
                .limit(limite)
                .toList();
    }

    // Acumulador interno para sumar unidades y dinero por producto
    private static final class Acumulado {
        private final Producto producto;
        private long unidades;
        private BigDecimal total = BigDecimal.ZERO;

        private Acumulado(Producto producto) {
            this.producto = producto;
        }
    }

    // Porcentaje de utilidad sobre los ingresos (ej: 25.50 = 25,5 %). Si no hay ingresos devuelve 0.
    public BigDecimal margenUtilidad(BigDecimal ingresos, BigDecimal utilidad) {
        if (ingresos == null || ingresos.signum() == 0 || utilidad == null) {
            return BigDecimal.ZERO;
        }
        return utilidad.multiply(BigDecimal.valueOf(100)).divide(ingresos, 2, RoundingMode.HALF_UP);
    }

    // Un tramo de la gráfica: el rango [inicio, fin] y sus totales.
    // cantidadVentas y valorVentas son solo de productos (sin citas), para la gráfica de ventas.
    public record PuntoSerie(LocalDate inicio, LocalDate fin, BigDecimal ingresos, BigDecimal gastos,
                             BigDecimal utilidad, long cantidadVentas, BigDecimal valorVentas) {
    }

    // Serie para las gráficas: parte el rango en tramos por DIA, SEMANA (lunes a domingo) o MES
    // y calcula los totales de cada tramo con las mismas reglas del resumen (ingresos, gastos y utilidad).
    // El primer y el último tramo se recortan al rango pedido.
    public List<PuntoSerie> serie(List<Venta> ventas, List<MascotaCita> citas, List<Gasto> gastos,
                                  LocalDate inicio, LocalDate fin, String agrupacion) {
        validarRango(inicio, fin);
        List<PuntoSerie> puntos = new ArrayList<>();
        LocalDate desde = inicio;
        while (!desde.isAfter(fin)) {
            if (puntos.size() >= MAX_TRAMOS_SERIE) {
                throw new BusinessRuleException("El rango es demasiado largo para esa agrupación; use semanas o meses");
            }
            final LocalDate tramoInicio = desde;
            final LocalDate tramoFin = finDelTramo(desde, fin, agrupacion);

            BigDecimal valorVentas = ingresosPorVentas(ventas, tramoInicio, tramoFin);
            BigDecimal ingresos = valorVentas.add(ingresosPorCitas(citas, tramoInicio, tramoFin));
            BigDecimal totalGastos = totalGastos(gastos, tramoInicio, tramoFin);
            long cantidadVentas = ventas == null ? 0 : ventas.stream()
                    .filter(v -> v != null && enRango(v.getFechaVenta(), tramoInicio, tramoFin))
                    .count();

            puntos.add(new PuntoSerie(tramoInicio, tramoFin, ingresos, totalGastos,
                    ingresos.subtract(totalGastos), cantidadVentas, valorVentas));
            desde = tramoFin.plusDays(1);
        }
        return puntos;
    }

    // Último día del tramo que empieza en 'desde' (sin pasarse de 'fin')
    private LocalDate finDelTramo(LocalDate desde, LocalDate fin, String agrupacion) {
        String tipo = agrupacion == null || agrupacion.isBlank() ? "DIA" : agrupacion.trim().toUpperCase();
        LocalDate finTramo = switch (tipo) {
            case "DIA" -> desde;
            case "SEMANA" -> desde.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            case "MES" -> desde.with(TemporalAdjusters.lastDayOfMonth());
            default -> throw new BusinessRuleException("La agrupación debe ser DIA, SEMANA o MES");
        };
        return finTramo.isAfter(fin) ? fin : finTramo;
    }

    private boolean detalleEnRango(DetalleVenta detalle, LocalDate inicio, LocalDate fin) {
        return detalle != null
                && detalle.getProducto() != null
                && detalle.getVenta() != null
                && enRango(detalle.getVenta().getFechaVenta(), inicio, fin);
    }

    private void validarRango(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new BusinessRuleException("Debe indicar la fecha inicial y final del reporte");
        }
        if (inicio.isAfter(fin)) {
            throw new BusinessRuleException("La fecha inicial no puede ser posterior a la fecha final");
        }
    }

    // Rango inclusivo: [inicio, fin]
    private boolean enRango(LocalDate fecha, LocalDate inicio, LocalDate fin) {
        return fecha != null && !fecha.isBefore(inicio) && !fecha.isAfter(fin);
    }
}