package usta.veterinaria.infrastructure.adapter.in.rest.mapper.admin;

import usta.veterinaria.domain.model.usuario.AgendamientoCita;
import usta.veterinaria.domain.model.admin.BitacoraServicio;
import usta.veterinaria.domain.model.admin.CategoriaGasto;
import usta.veterinaria.domain.model.admin.CategoriaProducto;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.admin.BitacoraServicioResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.admin.DetalleVentaRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.MapperUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// Arma la respuesta del panel de finanzas con los valores que ya calculó ConsultarFinanzasUseCase.
// No calcula nada: solo junta los valores y pasa las llaves de los mapas (enums) a texto.
public final class FinanzasRestMapper {

    private FinanzasRestMapper() {
    }

    public static DetalleVentaRequest.ResumenFinancieroResponse toResumenResponse(LocalDate inicio,
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
        return new DetalleVentaRequest.ResumenFinancieroResponse(
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

    // Convierte un servicio de la bitácora del dominio a la respuesta REST
    public static final class BitacoraServicioRestMapper {

        private BitacoraServicioRestMapper() {
        }

        public static BitacoraServicioResponse toResponse(BitacoraServicio servicio) {
            AgendamientoCita agendamiento = servicio.getAgendamientoCita();
            return new BitacoraServicioResponse(
                    servicio.getId(),
                    agendamiento == null ? null : agendamiento.getId(),
                    servicio.getTipoServicio(),
                    servicio.getFotoSalida(),
                    MapperUtils.nombre(servicio.getTipoBano()),
                    servicio.getPrecioSubtotal(),
                    servicio.getObservacionesPeluqueria());
        }

        public static List<BitacoraServicioResponse> toResponseList(List<BitacoraServicio> servicios) {
            return MapperUtils.lista(servicios, BitacoraServicioRestMapper::toResponse);
        }
    }
}
