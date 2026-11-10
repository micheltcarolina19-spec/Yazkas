package usta.veterinaria.application.usecase;

import usta.veterinaria.domain.model.mascota.Energia;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.domain.model.mascota.NivelAgresividad;
import usta.veterinaria.domain.model.mascota.Pelaje;
import usta.veterinaria.domain.model.mascota.Sexo;
import usta.veterinaria.domain.model.mascota.TipoMascota;
import usta.veterinaria.domain.model.usuario.Usuario;

import java.time.LocalDate;

// Arma una Mascota a partir de parámetros sueltos. La usan el registro de clientes y el de mascotas
// para no repetir la construcción de los 18 campos.
public final class MascotaFactory {

    private MascotaFactory() {
    }

    public static Mascota construir(Usuario usuario, String nombre, LocalDate fechaNacimiento, TipoMascota tipo,
                             String raza, Sexo sexo, Float tamano, Float peso, String color, Pelaje pelaje,
                             String observaciones, String fotoMascota, String temperamento,
                             NivelAgresividad nivelAgresividad, Boolean usaBozal, String fobias,
                             Boolean sociable, Energia energia) {
        return Mascota.builder()
                .usuario(usuario)
                .nombre(nombre)
                .fechaNacimiento(fechaNacimiento)
                .tipo(tipo)
                .raza(raza)
                .sexo(sexo)
                .tamano(tamano)
                .peso(peso)
                .color(color)
                .pelaje(pelaje)
                .observaciones(observaciones)
                .fotoMascota(fotoMascota)
                .temperamento(temperamento)
                .nivelAgresividad(nivelAgresividad)
                .usaBozal(usaBozal)
                .fobias(fobias)
                .sociable(sociable)
                .energia(energia)
                .build();
    }
}
