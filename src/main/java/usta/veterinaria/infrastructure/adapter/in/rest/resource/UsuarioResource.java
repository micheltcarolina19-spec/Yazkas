package usta.veterinaria.infrastructure.adapter.in.rest.resource;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import usta.veterinaria.application.port.in.GestionarCitasUseCase;
import usta.veterinaria.application.port.in.GestionarMascotasUseCase;
import usta.veterinaria.application.port.in.GestionarUsuariosUseCase;
import usta.veterinaria.domain.model.Mascota;
import usta.veterinaria.domain.model.MascotaCita;
import usta.veterinaria.domain.model.Usuario;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.ActualizarUsuarioRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.AgendarCitaRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.MascotaRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.MascotaResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.RegistroAdministradorRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.RegistroClienteRequest;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.UsuarioResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.MascotaCitaRestMapper;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.MascotaRestMapper;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.UsuarioRestMapper;

// import jakarta.annotation.security.RolesAllowed;   // <- descomentar cuando exista seguridad (login)

import java.util.List;

// Adaptador de entrada REST para los usuarios (clientes y administradores).
// También expone lo que cuelga de un usuario: registrar sus mascotas y agendar sus citas.
@Path("/api/usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Usuarios", description = "Registro de clientes y administradores, sus mascotas y sus citas")
public class UsuarioResource {

    @Inject
    GestionarUsuariosUseCase usuarios;

    @Inject
    GestionarMascotasUseCase mascotas;

    @Inject
    GestionarCitasUseCase citas;

    // Registra un cliente junto con su primera mascota y responde 201
    @POST
    @Path("/clientes")
    @Operation(summary = "Registrar un cliente con su primera mascota")
    public Response registrarCliente(@Valid @NotNull(message = "Los datos del cliente son obligatorios")
                                     RegistroClienteRequest request) {
        MascotaRequest mascota = request.mascota();

        // ============================================================================================
        // ===   CONTRASEÑA (pendiente)                                                             ===
        // ============================================================================================
        // Cuando agregues "String contrasena" al puerto GestionarUsuariosUseCase.registrarCliente
        // (después del correo) y al caso de uso, cambia la llamada de abajo por esta:
        //
        // Usuario usuario = usuarios.registrarCliente(
        //         request.nombre(), request.apellido(), request.celular(), request.correo(),
        //         request.contrasena(),
        //         mascota.nombre(), mascota.fechaNacimiento(), mascota.tipo(), mascota.raza(),
        //         mascota.sexo(), mascota.tamano(), mascota.peso(), mascota.color(), mascota.pelaje(),
        //         mascota.observaciones(), mascota.fotoMascota(), mascota.temperamento(),
        //         mascota.nivelAgresividad(), mascota.usaBozal(), mascota.fobias(), mascota.sociable(),
        //         mascota.energia());
        // ============================================================================================
        Usuario usuario = usuarios.registrarCliente(
                request.nombre(), request.apellido(), request.celular(), request.correo(),
                mascota.nombre(), mascota.fechaNacimiento(), mascota.tipo(), mascota.raza(),
                mascota.sexo(), mascota.tamano(), mascota.peso(), mascota.color(), mascota.pelaje(),
                mascota.observaciones(), mascota.fotoMascota(), mascota.temperamento(),
                mascota.nivelAgresividad(), mascota.usaBozal(), mascota.fobias(), mascota.sociable(),
                mascota.energia());
        // ============================================================================================

        return Response.status(Response.Status.CREATED).entity(UsuarioRestMapper.toResponse(usuario)).build();
    }

    // Registra un administrador y responde 201
    @POST
    @Path("/administradores")
    // @RolesAllowed("ADMIN")
    @Operation(summary = "Registrar un administrador")
    public Response registrarAdministrador(@Valid @NotNull(message = "Los datos del administrador son obligatorios")
                                           RegistroAdministradorRequest request) {

        // ============================================================================================
        // ===   CONTRASEÑA (pendiente)                                                             ===
        // ============================================================================================
        // Cuando agregues "String contrasena" al puerto GestionarUsuariosUseCase.registrarAdministrador
        // (después del correo) y al caso de uso, cambia la llamada de abajo por esta:
        //
        // Usuario usuario = usuarios.registrarAdministrador(
        //         request.nombre(), request.apellido(), request.celular(), request.correo(),
        //         request.contrasena());
        // ============================================================================================
        Usuario usuario = usuarios.registrarAdministrador(
                request.nombre(), request.apellido(), request.celular(), request.correo());
        // ============================================================================================

        return Response.status(Response.Status.CREATED).entity(UsuarioRestMapper.toResponse(usuario)).build();
    }

    // Actualiza los datos personales de un usuario (404 si no existe)
    @PUT
    @Path("/{id}")
    @Operation(summary = "Actualizar los datos de un usuario")
    public UsuarioResponse actualizar(@PathParam("id") Long id,
                                      @Valid @NotNull(message = "Los datos del usuario son obligatorios")
                                      ActualizarUsuarioRequest request) {
        Usuario usuario = usuarios.actualizar(id, request.nombre(), request.apellido(), request.celular(),
                request.correo());
        return UsuarioRestMapper.toResponse(usuario);
    }

    // Lista todos los usuarios
    @GET
    // @RolesAllowed("ADMIN")
    @Operation(summary = "Listar todos los usuarios")
    public List<UsuarioResponse> listar() {
        return UsuarioRestMapper.toResponseList(usuarios.listar());
    }

    // Busca un usuario por id (404 si no existe)
    @GET
    @Path("/{id}")
    @Operation(summary = "Buscar un usuario por id")
    public UsuarioResponse buscarPorId(@PathParam("id") Long id) {
        return UsuarioRestMapper.toResponse(usuarios.buscarPorId(id));
    }

    // Busca un usuario por correo: /api/usuarios/buscar?correo=ana@correo.com
    @GET
    @Path("/buscar")
    // @RolesAllowed("ADMIN")
    @Operation(summary = "Buscar un usuario por correo")
    public UsuarioResponse buscarPorCorreo(@QueryParam("correo")
                                           @NotBlank(message = "El correo es obligatorio") String correo) {
        return UsuarioRestMapper.toResponse(usuarios.buscarPorCorreo(correo));
    }

    // ------------------------------------ Mascotas del usuario ------------------------------------

    // Registra una nueva mascota para el usuario y responde 201
    @POST
    @Path("/{usuarioId}/mascotas")
    @Operation(summary = "Registrar una mascota para un usuario")
    public Response registrarMascota(@PathParam("usuarioId") Long usuarioId,
                                     @Valid @NotNull(message = "Los datos de la mascota son obligatorios")
                                     MascotaRequest request) {
        Mascota mascota = mascotas.registrar(
                usuarioId, request.nombre(), request.fechaNacimiento(), request.tipo(), request.raza(),
                request.sexo(), request.tamano(), request.peso(), request.color(), request.pelaje(),
                request.observaciones(), request.fotoMascota(), request.temperamento(),
                request.nivelAgresividad(), request.usaBozal(), request.fobias(), request.sociable(),
                request.energia());
        return Response.status(Response.Status.CREATED).entity(MascotaRestMapper.toResponse(mascota)).build();
    }

    // Lista las mascotas del usuario
    @GET
    @Path("/{usuarioId}/mascotas")
    @Operation(summary = "Listar las mascotas de un usuario")
    public List<MascotaResponse> listarMascotas(@PathParam("usuarioId") Long usuarioId) {
        return MascotaRestMapper.toResponseList(mascotas.listarPorUsuario(usuarioId));
    }

    // ------------------------------------ Citas del usuario ------------------------------------

    // Agenda una cita para una o varias mascotas del usuario y responde 201.
    // La duración (ALTA, MEDIA, BAJA) la estima la IA dentro del caso de uso; el cliente no ve precio.
    @POST
    @Path("/{usuarioId}/citas")
    @Operation(summary = "Agendar una cita para una o varias mascotas del usuario")
    public Response agendarCita(@PathParam("usuarioId") Long usuarioId,
                                @Valid @NotNull(message = "Los datos de la cita son obligatorios")
                                AgendarCitaRequest request) {
        List<MascotaCita> agendadas = citas.agendar(
                usuarioId, request.mascotaIds(), request.fechaAgendamiento(), request.horaAgendamiento());
        return Response.status(Response.Status.CREATED)
                .entity(MascotaCitaRestMapper.toResponseList(agendadas))
                .build();
    }
}
