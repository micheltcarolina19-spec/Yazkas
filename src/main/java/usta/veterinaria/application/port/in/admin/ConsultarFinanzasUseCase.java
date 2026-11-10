package usta.veterinaria.application.port.in.admin;

import usta.veterinaria.domain.model.admin.CategoriaGasto;
import usta.veterinaria.domain.model.admin.CategoriaProducto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

// Puerto primario: reportes financieros de un periodo (fechas inclusivas)
public interface ConsultarFinanzasUseCase {

    BigDecimal ingresosPorVentas(LocalDate inicio, LocalDate fin);

    // Solo cuentan las citas completadas
    BigDecimal ingresosPorCitas(LocalDate inicio, LocalDate fin);

    BigDecimal ingresosTotales(LocalDate inicio, LocalDate fin);

    BigDecimal totalGastos(LocalDate inicio, LocalDate fin);

    BigDecimal utilidad(LocalDate inicio, LocalDate fin);

    BigDecimal margenUtilidad(LocalDate inicio, LocalDate fin);

    Map<CategoriaGasto, BigDecimal> gastosPorCategoria(LocalDate inicio, LocalDate fin);

    Map<CategoriaProducto, BigDecimal> ventasPorCategoriaProducto(LocalDate inicio, LocalDate fin);

    BigDecimal gananciaProductos(LocalDate inicio, LocalDate fin);
}
