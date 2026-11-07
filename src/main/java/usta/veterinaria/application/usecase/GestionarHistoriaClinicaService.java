package usta.veterinaria.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.exception.ResourceNotFoundException;
import usta.veterinaria.application.port.in.GestionarHistoriaClinicaUseCase;
import usta.veterinaria.application.port.in.GestionarMascotasUseCase;
import usta.veterinaria.application.port.out.HistoriaClinicaRepository;
import usta.veterinaria.domain.model.HistoriaClinica;
import usta.veterinaria.domain.model.Mascota;
import usta.veterinaria.domain.service.MascotaService;

@ApplicationScoped
public class GestionarHistoriaClinicaService implements GestionarHistoriaClinicaUseCase {

    private final MascotaService mascotaService = new MascotaService();

    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final GestionarMascotasUseCase mascotas;

    @Inject
    public GestionarHistoriaClinicaService(HistoriaClinicaRepository historiaClinicaRepository,
                                           GestionarMascotasUseCase mascotas) {
        this.historiaClinicaRepository = historiaClinicaRepository;
        this.mascotas = mascotas;
    }

    @Override
    @Transactional
    public HistoriaClinica registrar(Long mascotaId, String alergiasMedicamentos, String alergiasCosmeticas,
                                     String alergiasAlimenticias, String enfermedadesCronicas,
                                     String condicionesFisicas, String problemasCardiacos,
                                     String problemasOrtopedicos, String diagnosticoDermatologico,
                                     Boolean presenciaParasitos, String cirugiasRecientes, String lesiones,
                                     Boolean esterilizacion, String observaciones) {
        Mascota mascota = mascotas.buscarPorId(mascotaId);

        HistoriaClinica nueva = construir(mascota, alergiasMedicamentos, alergiasCosmeticas,
                alergiasAlimenticias, enfermedadesCronicas, condicionesFisicas, problemasCardiacos,
                problemasOrtopedicos, diagnosticoDermatologico, presenciaParasitos, cirugiasRecientes,
                lesiones, esterilizacion, observaciones);

        HistoriaClinica existente = historiaClinicaRepository.findByMascotaId(mascota.getId()).orElse(null);
        return historiaClinicaRepository.save(mascotaService.validarHistoriaClinica(nueva, existente));
    }

    @Override
    @Transactional
    public HistoriaClinica actualizar(Long mascotaId, String alergiasMedicamentos, String alergiasCosmeticas,
                                      String alergiasAlimenticias, String enfermedadesCronicas,
                                      String condicionesFisicas, String problemasCardiacos,
                                      String problemasOrtopedicos, String diagnosticoDermatologico,
                                      Boolean presenciaParasitos, String cirugiasRecientes, String lesiones,
                                      Boolean esterilizacion, String observaciones) {
        HistoriaClinica existente = buscarPorMascota(mascotaId);

        HistoriaClinica actualizada = construir(existente.getMascota(), alergiasMedicamentos,
                alergiasCosmeticas, alergiasAlimenticias, enfermedadesCronicas, condicionesFisicas,
                problemasCardiacos, problemasOrtopedicos, diagnosticoDermatologico, presenciaParasitos,
                cirugiasRecientes, lesiones, esterilizacion, observaciones);
        actualizada.setId(existente.getId());

        return historiaClinicaRepository.save(mascotaService.validarHistoriaClinica(actualizada, existente));
    }

    @Override
    public HistoriaClinica buscarPorMascota(Long mascotaId) {
        return historiaClinicaRepository.findByMascotaId(Validaciones.requerirId(mascotaId, "id de la mascota"))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La mascota con id " + mascotaId + " no tiene historia clínica"));
    }

    private HistoriaClinica construir(Mascota mascota, String alergiasMedicamentos, String alergiasCosmeticas,
                                      String alergiasAlimenticias, String enfermedadesCronicas,
                                      String condicionesFisicas, String problemasCardiacos,
                                      String problemasOrtopedicos, String diagnosticoDermatologico,
                                      Boolean presenciaParasitos, String cirugiasRecientes, String lesiones,
                                      Boolean esterilizacion, String observaciones) {
        return HistoriaClinica.builder()
                .mascota(mascota)
                .alergiasMedicamentos(alergiasMedicamentos)
                .alergiasCosmeticas(alergiasCosmeticas)
                .alergiasAlimenticias(alergiasAlimenticias)
                .enfermedadesCronicas(enfermedadesCronicas)
                .condicionesFisicas(condicionesFisicas)
                .problemasCardiacos(problemasCardiacos)
                .problemasOrtopedicos(problemasOrtopedicos)
                .diagnosticoDermatologico(diagnosticoDermatologico)
                .presenciaParasitos(presenciaParasitos)
                .cirugiasRecientes(cirugiasRecientes)
                .lesiones(lesiones)
                .esterilizacion(esterilizacion)
                .observaciones(observaciones)
                .build();
    }
}
