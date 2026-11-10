package usta.veterinaria.application.usecase.admin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.port.in.admin.GestionarBitacoraServicioUseCase;
import usta.veterinaria.application.port.in.usuario.GestionarCitasUseCase;
import usta.veterinaria.application.port.out.admin.BitacoraServicioRepository;
import usta.veterinaria.domain.model.usuario.AgendamientoCita;
import usta.veterinaria.domain.model.admin.BitacoraServicio;
import usta.veterinaria.domain.model.admin.TipoBano;
import usta.veterinaria.domain.service.admin.BitacoraServicioService;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class GestionarBitacoraServicioService implements GestionarBitacoraServicioUseCase {

    private final BitacoraServicioService bitacoraService = new BitacoraServicioService();

    private final BitacoraServicioRepository bitacoraRepository;
    private final GestionarCitasUseCase citas;

    @Inject
    public GestionarBitacoraServicioService(BitacoraServicioRepository bitacoraRepository,
                                            GestionarCitasUseCase citas) {
        this.bitacoraRepository = bitacoraRepository;
        this.citas = citas;
    }

    @Override
    @Transactional
    public BitacoraServicio registrar(Long agendamientoId, String tipoServicio, String fotoSalida,
                                      TipoBano tipoBano, BigDecimal precioSubtotal,
                                      String observacionesPeluqueria) {
        AgendamientoCita agendamiento = citas.buscarPorId(agendamientoId);

        BitacoraServicio servicio = BitacoraServicio.builder()
                .tipoServicio(tipoServicio)
                .fotoSalida(fotoSalida)
                .tipoBano(tipoBano)
                .precioSubtotal(precioSubtotal)
                .observacionesPeluqueria(observacionesPeluqueria)
                .build();

        return bitacoraRepository.save(bitacoraService.registrar(agendamiento, servicio));
    }

    @Override
    public List<BitacoraServicio> listarPorAgendamiento(Long agendamientoId) {
        citas.buscarPorId(agendamientoId);
        return bitacoraRepository.findByAgendamientoCitaId(agendamientoId);
    }
}
