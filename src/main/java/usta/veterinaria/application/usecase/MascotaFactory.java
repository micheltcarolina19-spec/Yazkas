package usta.veterinaria.application.usecase;

import usta.veterinaria.domain.model.Energia;
import usta.veterinaria.domain.model.Mascota;
import usta.veterinaria.domain.model.NivelAgresividad;
import usta.veterinaria.domain.model.Pelaje;
import usta.veterinaria.domain.model.Sexo;
import usta.veterinaria.domain.model.TipoMascota;
import usta.veterinaria.domain.model.Usuario;

import java.time.LocalDate;

// Arma una Mascota a partir de parámetros sueltos. La usan el registro de clientes y el de mascotas
// para no repetir la construcción de los 18 campos.
final class MascotaFactory {

    private MascotaFactory() {
    }

    static Mascota construir(Usuario usuario, String nombre, LocalDate fechaNacimiento, TipoMascota tipo,
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
