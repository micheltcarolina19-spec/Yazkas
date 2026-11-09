package usta.veterinaria.infrastructure.adapter.in.rest.mapper;

import usta.veterinaria.domain.model.HistoriaClinica;
import usta.veterinaria.domain.model.Mascota;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.HistoriaClinicaResponse;

// Convierte la historia clínica del dominio a la respuesta REST
public final class HistoriaClinicaRestMapper {

    private HistoriaClinicaRestMapper() {
    }

    public static HistoriaClinicaResponse toResponse(HistoriaClinica historia) {
        Mascota mascota = historia.getMascota();
        return new HistoriaClinicaResponse(
                historia.getId(),
                mascota == null ? null : mascota.getId(),
                mascota == null ? null : mascota.getNombre(),
                historia.getAlergiasMedicamentos(),
                historia.getAlergiasCosmeticas(),
                historia.getAlergiasAlimenticias(),
                historia.getEnfermedadesCronicas(),
                historia.getCondicionesFisicas(),
                historia.getProblemasCardiacos(),
                historia.getProblemasOrtopedicos(),
                historia.getDiagnosticoDermatologico(),
                historia.getPresenciaParasitos(),
                historia.getCirugiasRecientes(),
                historia.getLesiones(),
                historia.getEsterilizacion(),
                historia.getObservaciones());
    }
}
