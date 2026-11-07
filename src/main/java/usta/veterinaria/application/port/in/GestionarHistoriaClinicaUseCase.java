package usta.veterinaria.application.port.in;

import usta.veterinaria.domain.model.HistoriaClinica;

// Puerto primario: historia clínica de una mascota (máximo una por mascota)
public interface GestionarHistoriaClinicaUseCase {

    HistoriaClinica registrar(Long mascotaId, String alergiasMedicamentos, String alergiasCosmeticas,
                              String alergiasAlimenticias, String enfermedadesCronicas,
                              String condicionesFisicas, String problemasCardiacos,
                              String problemasOrtopedicos, String diagnosticoDermatologico,
                              Boolean presenciaParasitos, String cirugiasRecientes, String lesiones,
                              Boolean esterilizacion, String observaciones);

    HistoriaClinica actualizar(Long mascotaId, String alergiasMedicamentos, String alergiasCosmeticas,
                               String alergiasAlimenticias, String enfermedadesCronicas,
                               String condicionesFisicas, String problemasCardiacos,
                               String problemasOrtopedicos, String diagnosticoDermatologico,
                               Boolean presenciaParasitos, String cirugiasRecientes, String lesiones,
                               Boolean esterilizacion, String observaciones);

    HistoriaClinica buscarPorMascota(Long mascotaId);
}
