package usta.veterinaria.application.port.in.mascota;

import usta.veterinaria.domain.model.mascota.Energia;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.domain.model.mascota.NivelAgresividad;
import usta.veterinaria.domain.model.mascota.Pelaje;
import usta.veterinaria.domain.model.mascota.Sexo;
import usta.veterinaria.domain.model.mascota.TipoMascota;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

// Puerto primario: gestión de mascotas
public interface GestionarMascotasUseCase {

    Mascota registrar(Long usuarioId, String nombre, LocalDate fechaNacimiento, TipoMascota tipo, String raza,
                      Sexo sexo, Float tamano, Float peso, String color, Pelaje pelaje, String observaciones,
                      String fotoMascota, String temperamento, NivelAgresividad nivelAgresividad,
                      Boolean usaBozal, String fobias, Boolean sociable, Energia energia);

    Mascota actualizar(Long id, String nombre, LocalDate fechaNacimiento, TipoMascota tipo, String raza,
                       Sexo sexo, Float tamano, Float peso, String color, Pelaje pelaje, String observaciones,
                       String fotoMascota, String temperamento, NivelAgresividad nivelAgresividad,
                       Boolean usaBozal, String fobias, Boolean sociable, Energia energia);

    // No permite eliminar la única mascota de un cliente
    void eliminar(Long id);

    Mascota buscarPorId(Long id);

    List<Mascota> listar();

    List<Mascota> listarPorUsuario(Long usuarioId);

    Period calcularEdad(Long id);
}
