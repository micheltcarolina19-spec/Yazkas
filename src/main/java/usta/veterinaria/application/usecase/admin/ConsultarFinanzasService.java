package usta.veterinaria.application.usecase.admin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import usta.veterinaria.application.port.in.admin.ConsultarFinanzasUseCase;
import usta.veterinaria.application.port.out.admin.DetalleVentaRepository;
import usta.veterinaria.application.port.out.admin.GastoRepository;
import usta.veterinaria.application.port.out.mascota.MascotaCitaRepository;
import usta.veterinaria.application.port.out.admin.VentaRepository;
import usta.veterinaria.application.usecase.Validaciones;
import usta.veterinaria.domain.model.admin.CategoriaGasto;
import usta.veterinaria.domain.model.admin.CategoriaProducto;
import usta.veterinaria.domain.model.admin.Gasto;
import usta.veterinaria.domain.service.admin.FinanzasService.ProductoVendido;
import usta.veterinaria.domain.service.admin.FinanzasService.PuntoSerie;
import usta.veterinaria.domain.model.usuario.MascotaCita;
import usta.veterinaria.domain.model.admin.Venta;
import usta.veterinaria.domain.service.admin.FinanzasService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// Solo consulta: carga los datos del periodo y deja que el dominio haga los cálculos
@ApplicationScoped
public class ConsultarFinanzasService implements ConsultarFinanzasUseCase {

    private final FinanzasService finanzasService = new FinanzasService();

    private final VentaRepository ventaRepository;
    private final MascotaCitaRepository mascotaCitaRepository;
    private final GastoRepository gastoRepository;
    private final DetalleVentaRepository detalleVentaRepository;

    @Inject
    public ConsultarFinanzasService(VentaRepository ventaRepository,
                                    MascotaCitaRepository mascotaCitaRepository,
                                    GastoRepository gastoRepository,
                                    DetalleVentaRepository detalleVentaRepository) {
        this.ventaRepository = ventaRepository;
        this.mascotaCitaRepository = mascotaCitaRepository;
        this.gastoRepository = gastoRepository;
        this.detalleVentaRepository = detalleVentaRepository;
    }

    @Override
    public BigDecimal ingresosPorVentas(LocalDate inicio, LocalDate fin) {
        return finanzasService.ingresosPorVentas(ventas(inicio, fin), inicio, fin);
    }

    @Override
    public BigDecimal ingresosPorCitas(LocalDate inicio, LocalDate fin) {
        return finanzasService.ingresosPorCitas(citas(inicio, fin), inicio, fin);
    }

    @Override
    public BigDecimal ingresosTotales(LocalDate inicio, LocalDate fin) {
        return finanzasService.ingresosTotales(ventas(inicio, fin), citas(inicio, fin), inicio, fin);
    }

    @Override
    public BigDecimal totalGastos(LocalDate inicio, LocalDate fin) {
        return finanzasService.totalGastos(gastos(inicio, fin), inicio, fin);
    }

    @Override
    public BigDecimal utilidad(LocalDate inicio, LocalDate fin) {
        return finanzasService.utilidad(ventas(inicio, fin), citas(inicio, fin), gastos(inicio, fin),
                inicio, fin);
    }

    @Override
    public BigDecimal margenUtilidad(LocalDate inicio, LocalDate fin) {
        // Se cargan los datos una sola vez para que ingresos y utilidad salgan del mismo corte
        List<Venta> ventas = ventas(inicio, fin);
        List<MascotaCita> citas = citas(inicio, fin);
        List<Gasto> gastos = gastos(inicio, fin);

        BigDecimal ingresos = finanzasService.ingresosTotales(ventas, citas, inicio, fin);
        BigDecimal utilidad = finanzasService.utilidad(ventas, citas, gastos, inicio, fin);
        return finanzasService.margenUtilidad(ingresos, utilidad);
    }

    @Override
    public Map<CategoriaGasto, BigDecimal> gastosPorCategoria(LocalDate inicio, LocalDate fin) {
        return finanzasService.gastosPorCategoria(gastos(inicio, fin), inicio, fin);
    }

    @Override
    public Map<CategoriaProducto, BigDecimal> ventasPorCategoriaProducto(LocalDate inicio, LocalDate fin) {
        Validaciones.rangoFechas(inicio, fin);
        return finanzasService.ventasPorCategoriaProducto(
                detalleVentaRepository.findByVentaFechaVentaBetween(inicio, fin), inicio, fin);
    }

    @Override
    public BigDecimal gananciaProductos(LocalDate inicio, LocalDate fin) {
        Validaciones.rangoFechas(inicio, fin);
        return finanzasService.gananciaProductos(
                detalleVentaRepository.findByVentaFechaVentaBetween(inicio, fin), inicio, fin);
    }

    @Override
    public List<ProductoVendido> productosMasVendidos(LocalDate inicio, LocalDate fin, int limite) {
        Validaciones.rangoFechas(inicio, fin);
        return finanzasService.productosMasVendidos(
                detalleVentaRepository.findByVentaFechaVentaBetween(inicio, fin), inicio, fin, limite);
    }

    // Se cargan ventas, citas y gastos una sola vez y el dominio los reparte por tramo
    @Override
    public List<PuntoSerie> serieFinanciera(LocalDate inicio, LocalDate fin, String agrupacion) {
        return finanzasService.serie(ventas(inicio, fin), citas(inicio, fin), gastos(inicio, fin),
                inicio, fin, agrupacion);
    }

    private List<Venta> ventas(LocalDate inicio, LocalDate fin) {
        Validaciones.rangoFechas(inicio, fin);
        return ventaRepository.findByFechaVentaBetween(inicio, fin);
    }

    private List<MascotaCita> citas(LocalDate inicio, LocalDate fin) {
        Validaciones.rangoFechas(inicio, fin);
        return mascotaCitaRepository.findByFechaAgendamientoBetween(inicio, fin);
    }

    private List<Gasto> gastos(LocalDate inicio, LocalDate fin) {
        Validaciones.rangoFechas(inicio, fin);
        return gastoRepository.findByFechaGastoBetween(inicio, fin);
    }
}