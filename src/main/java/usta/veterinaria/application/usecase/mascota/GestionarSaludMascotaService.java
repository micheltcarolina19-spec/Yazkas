package usta.veterinaria.application.usecase.mascota;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.port.in.mascota.GestionarMascotasUseCase;
import usta.veterinaria.application.port.in.mascota.GestionarSaludMascotaUseCase;
import usta.veterinaria.application.port.out.mascota.DesparasitacionRepository;
import usta.veterinaria.application.port.out.mascota.VacunacionRepository;
import usta.veterinaria.domain.model.mascota.Desparasitacion;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.domain.model.mascota.TipoDesparasitacion;
import usta.veterinaria.domain.model.mascota.Vacunacion;
import usta.veterinaria.domain.service.macota.SaludMascotaService;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class GestionarSaludMascotaService implements GestionarSaludMascotaUseCase {

    private final SaludMascotaService saludService = new SaludMascotaService();

    private final VacunacionRepository vacunacionRepository;
    private final DesparasitacionRepository desparasitacionRepository;
    private final GestionarMascotasUseCase mascotas;

    @Inject
    public GestionarSaludMascotaService(VacunacionRepository vacunacionRepository,
                                        DesparasitacionRepository desparasitacionRepository,
                                        GestionarMascotasUseCase mascotas) {
        this.vacunacionRepository = vacunacionRepository;
        this.desparasitacionRepository = desparasitacionRepository;
        this.mascotas = mascotas;
    }

    @Override
    @Transactional
    public Vacunacion registrarVacunacion(Long mascotaId, String nombreVacuna, LocalDate fechaAplicacion,
                                          LocalDate fechaRefuerzo, String numeroLote, Integer numeroDosis,
                                          String observaciones) {
        Mascota mascota = mascotas.buscarPorId(mascotaId);

        Vacunacion vacunacion = Vacunacion.builder()
                .mascota(mascota)
                .nombreVacuna(nombreVacuna)
                .fechaAplicacion(fechaAplicacion)
                .fechaRefuerzo(fechaRefuerzo)
                .numeroLote(numeroLote)
                .numeroDosis(numeroDosis)
                .observaciones(observaciones)
                .build();

        return vacunacionRepository.save(saludService.registrarVacunacion(vacunacion));
    }

    @Override
    @Transactional
    public Desparasitacion registrarDesparasitacion(Long mascotaId, String marca, LocalDate fechaAplicacion,
                                                    TipoDesparasitacion tipo, String numeroLote,
                                                    String observaciones) {
        Mascota mascota = mascotas.buscarPorId(mascotaId);

        Desparasitacion desparasitacion = Desparasitacion.builder()
                .mascota(mascota)
                .marca(marca)
                .fechaAplicacion(fechaAplicacion)
                .tipo(tipo)
                .numeroLote(numeroLote)
                .observaciones(observaciones)
                .build();

        return desparasitacionRepository.save(saludService.registrarDesparasitacion(desparasitacion));
    }

    @Override
    public List<Vacunacion> listarVacunaciones(Long mascotaId) {
        return vacunacionRepository.findByMascotaId(mascotas.buscarPorId(mascotaId).getId());
    }

    @Override
    public List<Desparasitacion> listarDesparasitaciones(Long mascotaId) {
        return desparasitacionRepository.findByMascotaId(mascotas.buscarPorId(mascotaId).getId());
    }

    @Override
    public List<Vacunacion> refuerzosProximos(int dias) {
        return saludService.refuerzosProximos(vacunacionRepository.findConRefuerzo(), dias);
    }

    @Override
    public List<Vacunacion> refuerzosVencidos() {
        return saludService.refuerzosVencidos(vacunacionRepository.findConRefuerzo());
    }
}
