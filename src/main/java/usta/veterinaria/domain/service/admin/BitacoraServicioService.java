package usta.veterinaria.domain.service.admin;

import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.admin.BitacoraServicio;
import usta.veterinaria.domain.model.usuario.AgendamientoCita;
import usta.veterinaria.domain.model.usuario.EstadoCita;
import usta.veterinaria.domain.model.usuario.MascotaCita;
import usta.veterinaria.domain.service.MonedaColombiana;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

// Servicio que registra lo que se le hizo a la mascota en la cita y cuánto se cobró.
// El administrador lo registra al terminar la cita o al final del día.
public class BitacoraServicioService {

    // Servicios que se pueden prestar en una cita (se pueden combinar)
    public static final String BANO = "Baño";
    public static final String PELUQUERIA = "Peluquería";
    public static final String VACUNACION = "Vacunación";
    public static final String DESPARASITACION = "Desparasitación";
    public static final List<String> SERVICIOS = List.of(BANO, PELUQUERIA, VACUNACION, DESPARASITACION);

    // Registra un servicio (baño, corte, etc.) con su valor en pesos colombianos
    public BitacoraServicio registrar(AgendamientoCita agendamiento, BitacoraServicio servicio) {
        if (agendamiento == null) {
            throw new BusinessRuleException("La cita no existe");
        }
        if (agendamiento.getEstado() == EstadoCita.CANCELADA) {
            throw new BusinessRuleException("No se pueden registrar servicios en una cita cancelada");
        }
        if (agendamiento.getEstado() == EstadoCita.COMPLETADA) {
            throw new BusinessRuleException("La cita ya fue completada, no se le pueden agregar más servicios");
        }
        if (servicio == null) {
            throw new BusinessRuleException("El servicio es obligatorio");
        }
        if (servicio.getTipoServicio() == null || servicio.getTipoServicio().isBlank()) {
            throw new BusinessRuleException("Debe indicar el tipo de servicio realizado");
        }

        servicio.setTipoServicio(servicio.getTipoServicio().trim());
        servicio.setPrecioSubtotal(MonedaColombiana.validarMontoPositivo(
                servicio.getPrecioSubtotal(), "precio del servicio"));
        servicio.setAgendamientoCita(agendamiento);
        return servicio;
    }

    // Registra la bitácora de UNA mascota de la cita (una cita puede tener hasta dos mascotas).
    // Valida que la mascota pertenezca a la cita y que los servicios sean Baño / Peluquería / Vacunación /
    // Desparasitación. Si se marcó Vacunación o Desparasitación, deben venir sus datos (van a sus propias tablas).
    public BitacoraServicio registrarParaMascota(AgendamientoCita agendamiento, MascotaCita mascotaCita,
                                                 BitacoraServicio servicio) {
        if (mascotaCita == null || mascotaCita.getId() == null) {
            throw new BusinessRuleException("Debe indicar la mascota de la cita");
        }
        if (mascotaCita.getAgendamientoCita() != null && agendamiento != null
                && !Objects.equals(mascotaCita.getAgendamientoCita().getId(), agendamiento.getId())) {
            throw new BusinessRuleException("La mascota no pertenece a esta cita");
        }
        if (servicio == null) {
            throw new BusinessRuleException("El servicio es obligatorio");
        }

        List<String> servicios = serviciosDe(servicio.getTipoServicio());
        if (servicios.isEmpty()) {
            throw new BusinessRuleException(
                    "Selecciona al menos un tipo de servicio (Baño, Peluquería, Vacunación o Desparasitación)");
        }
        servicio.setTipoServicio(String.join(", ", servicios));

        // El tipo de baño solo aplica cuando se prestó el servicio de baño
        if (servicios.contains(BANO)) {
            if (servicio.getTipoBano() == null) {
                throw new BusinessRuleException("Selecciona el tipo de baño realizado");
            }
        } else {
            servicio.setTipoBano(null);
        }

        // Vacunación y desparasitación se registran en el carnet de la mascota (tablas propias)
        if (servicios.contains(VACUNACION)) {
            if (servicio.getVacunacion() == null) {
                throw new BusinessRuleException("Completa los datos de la vacuna aplicada");
            }
        } else {
            servicio.setVacunacion(null);
        }
        if (servicios.contains(DESPARASITACION)) {
            if (servicio.getDesparasitacion() == null) {
                throw new BusinessRuleException("Completa los datos de la desparasitación aplicada");
            }
        } else {
            servicio.setDesparasitacion(null);
        }

        if (servicio.getFotoSalida() != null) {
            String foto = servicio.getFotoSalida().trim();
            if (foto.length() > 255) {
                throw new BusinessRuleException("La URL de la foto no puede superar 255 caracteres");
            }
            servicio.setFotoSalida(foto.isEmpty() ? null : foto);
        }
        if (servicio.getObservacionesPeluqueria() != null && servicio.getObservacionesPeluqueria().length() > 1000) {
            throw new BusinessRuleException("Las observaciones no pueden superar 1000 caracteres");
        }

        BitacoraServicio registrado = registrar(agendamiento, servicio);
        registrado.setMascotaCita(mascotaCita);
        return registrado;
    }

    // Convierte "Baño, Peluquería" en la lista de servicios válidos, sin repetidos y en orden fijo
    public List<String> serviciosDe(String tipoServicio) {
        if (tipoServicio == null || tipoServicio.isBlank()) {
            return List.of();
        }
        Set<String> elegidos = new LinkedHashSet<>();
        for (String parte : tipoServicio.split(",")) {
            String limpio = parte.trim();
            if (limpio.isEmpty()) {
                continue;
            }
            String valido = SERVICIOS.stream().filter(s -> s.equalsIgnoreCase(limpio)).findFirst()
                    .orElseThrow(() -> new BusinessRuleException("Servicio no válido: " + limpio));
            elegidos.add(valido);
        }
        List<String> ordenados = new ArrayList<>();
        for (String s : SERVICIOS) {
            if (elegidos.contains(s)) {
                ordenados.add(s);
            }
        }
        return ordenados;
    }

    // true si TODAS las mascotas de la cita ya tienen su bitácora con precio -> la cita se puede completar
    public boolean todasLasMascotasRegistradas(List<MascotaCita> mascotasDeLaCita, List<BitacoraServicio> bitacoras) {
        if (mascotasDeLaCita == null || mascotasDeLaCita.isEmpty()) {
            return false;
        }
        for (MascotaCita cita : mascotasDeLaCita) {
            if (buscarDeMascota(bitacoras, cita.getId()) == null) {
                return false;
            }
        }
        return true;
    }

    // Bitácora (con precio) de una mascota de la cita, o null si aún no se ha registrado
    public BitacoraServicio buscarDeMascota(List<BitacoraServicio> bitacoras, Long mascotaCitaId) {
        if (bitacoras == null || mascotaCitaId == null) {
            return null;
        }
        for (BitacoraServicio b : bitacoras) {
            if (b != null && b.getMascotaCita() != null && mascotaCitaId.equals(b.getMascotaCita().getId())
                    && b.getPrecioSubtotal() != null && b.getPrecioSubtotal().signum() > 0) {
                return b;
            }
        }
        return null;
    }

    // Suma de los subtotales registrados (total de la cita, aunque aún no esté completada)
    public BigDecimal total(List<BitacoraServicio> bitacoras) {
        BigDecimal total = BigDecimal.ZERO;
        if (bitacoras != null) {
            for (BitacoraServicio b : bitacoras) {
                if (b != null && b.getPrecioSubtotal() != null) {
                    total = total.add(b.getPrecioSubtotal());
                }
            }
        }
        return MonedaColombiana.normalizar(total);
    }
}
