package usta.veterinaria.infrastructure.adapter.in.rest.mapper;

import usta.veterinaria.domain.model.CategoriaGasto;
import usta.veterinaria.domain.model.CategoriaProducto;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.ResumenFinancieroResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

// Arma la respuesta del panel de finanzas con los valores que ya calculó ConsultarFinanzasUseCase.
// No calcula nada: solo junta los valores y pasa las llaves de los mapas (enums) a texto.
public final class FinanzasRestMapper {

    private FinanzasRestMapper() {
    }

    public static ResumenFinancieroResponse toResumenResponse(LocalDate inicio,
                                                              LocalDate fin,
                                                              BigDecimal ingresosPorVentas,
                                                              BigDecimal ingresosPorCitas,
                                                              BigDecimal ingresosTotales,
                                                              BigDecimal totalGastos,
                                                              BigDecimal utilidad,
                                                              BigDecimal margenUtilidad,
                                                              BigDecimal gananciaProductos,
                                                              Map<CategoriaGasto, BigDecimal> gastosPorCategoria,
                                                              Map<CategoriaProducto, BigDecimal> ventasPorCategoriaProducto) {
        return new ResumenFinancieroResponse(
                inicio,
                fin,
                ingresosPorVentas,
                ingresosPorCitas,
                ingresosTotales,
                totalGastos,
                utilidad,
                margenUtilidad,
                gananciaProductos,
                MapperUtils.mapaPorNombre(gastosPorCategoria),
                MapperUtils.mapaPorNombre(ventasPorCategoriaProducto));
    }
}
