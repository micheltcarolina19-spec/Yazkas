package usta.veterinaria.infrastructure.adapter.in.web;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.stream.Stream;
import usta.veterinaria.application.port.in.mascota.GestionarHistoriaClinicaUseCase;
import usta.veterinaria.application.port.in.mascota.GestionarMascotasUseCase;
import usta.veterinaria.application.port.in.usuario.GestionarUsuariosUseCase;
import usta.veterinaria.domain.model.mascota.*;
import usta.veterinaria.domain.model.usuario.Usuario;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.mascota.DesparasitacionResponse.MascotaRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.mascota.HistoriaClinicaRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.usuario.RegistroClienteRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.usuario.UsuarioRestMapper;

@Path("/registro")
public class RegistroResource {

    @Inject
    @Location("registro.html")
    Template registro;

    @Inject
    GestionarUsuariosUseCase usuarios;

    @Inject
    GestionarMascotasUseCase mascotas;

    @Inject
    GestionarHistoriaClinicaUseCase historias;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance mostrar() {
        // El paso 4 (historia clínica) es texto libre y booleanos: no necesita datos del servidor
        return registro.data("tipos", TipoMascota.values())
                .data("sexos", Sexo.values())
                .data("pelajes", Pelaje.values())
                .data("energias", Energia.values())
                .data("agresividades", NivelAgresividad.values());
    }

    // Crea el cliente + su primera mascota + (opcional) la historia clínica, todo en una transacción.
    // Si cualquier paso falla se revierte todo, así el usuario puede reintentar sin "correo ya registrado".
    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrar(@Valid @NotNull RegistroClienteRequest request) {
        MascotaRequest m = request.mascota();
        Usuario cliente = usuarios.registrarCliente(
                request.nombre(), request.apellido(), request.celular(), request.correo(), request.contrasena(),
                m.nombre(), m.fechaNacimiento(), m.tipo(), m.raza(), m.sexo(), m.tamano(), m.peso(),
                m.color(), m.pelaje(), m.observaciones(), m.fotoMascota(), m.temperamento(),
                m.nivelAgresividad(), m.usaBozal(), m.fobias(), m.sociable(), m.energia());

        HistoriaClinicaRequest h = request.historiaClinica();
        if (tieneContenido(h)) {
            // El cliente recién creado tiene exactamente una mascota
            Mascota primera = mascotas.listarPorUsuario(cliente.getId()).get(0);
            historias.registrar(
                    primera.getId(), h.alergiasMedicamentos(), h.alergiasCosmeticas(),
                    h.alergiasAlimenticias(), h.enfermedadesCronicas(), h.condicionesFisicas(),
                    h.problemasCardiacos(), h.problemasOrtopedicos(), h.diagnosticoDermatologico(),
                    h.presenciaParasitos(), h.cirugiasRecientes(), h.lesiones(),
                    h.esterilizacion(), h.observaciones());
        }
        return Response.status(Response.Status.CREATED).entity(UsuarioRestMapper.toResponse(cliente)).build();
    }

    // Evita guardar una historia vacía si el usuario saltó el paso
    private static boolean tieneContenido(HistoriaClinicaRequest h) {
        if (h == null) return false;
        boolean texto = Stream.of(h.alergiasMedicamentos(), h.alergiasCosmeticas(), h.alergiasAlimenticias(),
                        h.enfermedadesCronicas(), h.condicionesFisicas(), h.problemasCardiacos(),
                        h.problemasOrtopedicos(), h.diagnosticoDermatologico(), h.cirugiasRecientes(),
                        h.lesiones(), h.observaciones())
                .anyMatch(s -> s != null && !s.isBlank());
        return texto || h.presenciaParasitos() != null || h.esterilizacion() != null;
    }
}