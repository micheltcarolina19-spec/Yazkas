package usta.veterinaria.infrastructure.adapter.in.rest.resource;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import usta.veterinaria.application.port.in.GestionarHistoriaClinicaUseCase;
import usta.veterinaria.domain.model.HistoriaClinica;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.HistoriaClinicaRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.HistoriaClinicaResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.HistoriaClinicaRestMapper;

// Adaptador de entrada REST para la historia clínica de una mascota (una por mascota)
@Path("/api/mascotas/{mascotaId}/historia-clinica")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Historia clinica", description = "Historia clínica de cada mascota")
public class HistoriaClinicaResource {

    @Inject
    GestionarHistoriaClinicaUseCase historias;

    // Registra la historia clínica de la mascota y responde 201 (400 si ya tiene una)
    @POST
    @Operation(summary = "Registrar la historia clínica de una mascota")
    public Response registrar(@PathParam("mascotaId") Long mascotaId,
                              @Valid @NotNull(message = "Los datos de la historia clínica son obligatorios")
                              HistoriaClinicaRequest request) {
        HistoriaClinica historia = historias.registrar(
                mascotaId, request.alergiasMedicamentos(), request.alergiasCosmeticas(),
                request.alergiasAlimenticias(), request.enfermedadesCronicas(), request.condicionesFisicas(),
                request.problemasCardiacos(), request.problemasOrtopedicos(), request.diagnosticoDermatologico(),
                request.presenciaParasitos(), request.cirugiasRecientes(), request.lesiones(),
                request.esterilizacion(), request.observaciones());
        return Response.status(Response.Status.CREATED)
                .entity(HistoriaClinicaRestMapper.toResponse(historia))
                .build();
    }

    // Actualiza la historia clínica de la mascota (404 si no tiene)
    @PUT
    @Operation(summary = "Actualizar la historia clínica de una mascota")
    public HistoriaClinicaResponse actualizar(@PathParam("mascotaId") Long mascotaId,
                                              @Valid @NotNull(message = "Los datos de la historia clínica son obligatorios")
                                              HistoriaClinicaRequest request) {
        HistoriaClinica historia = historias.actualizar(
                mascotaId, request.alergiasMedicamentos(), request.alergiasCosmeticas(),
                request.alergiasAlimenticias(), request.enfermedadesCronicas(), request.condicionesFisicas(),
                request.problemasCardiacos(), request.problemasOrtopedicos(), request.diagnosticoDermatologico(),
                request.presenciaParasitos(), request.cirugiasRecientes(), request.lesiones(),
                request.esterilizacion(), request.observaciones());
        return HistoriaClinicaRestMapper.toResponse(historia);
    }

    // Consulta la historia clínica de la mascota (404 si no tiene)
    @GET
    @Operation(summary = "Consultar la historia clínica de una mascota")
    public HistoriaClinicaResponse buscar(@PathParam("mascotaId") Long mascotaId) {
        return HistoriaClinicaRestMapper.toResponse(historias.buscarPorMascota(mascotaId));
    }
}
