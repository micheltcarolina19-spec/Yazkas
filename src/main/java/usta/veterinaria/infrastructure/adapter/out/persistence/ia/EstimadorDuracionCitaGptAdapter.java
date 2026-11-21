package usta.veterinaria.infrastructure.adapter.out.persistence.ia;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import usta.veterinaria.application.port.out.usuario.EstimadorDuracionCitaPort;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.domain.model.mascota.Pelaje;
import usta.veterinaria.domain.model.mascota.TipoMascota;
import usta.veterinaria.domain.model.usuario.DuracionCita;
import usta.veterinaria.infrastructure.adapter.out.persistence.ia.client.OpenAiClient;
import usta.veterinaria.infrastructure.adapter.out.persistence.ia.client.dto.OpenAiRequest;
import usta.veterinaria.infrastructure.adapter.out.persistence.ia.client.dto.OpenAiResponse;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estima la duración de la cita (BAJA, MEDIA, ALTA) con GPT.
 *
 * La IA se guía por la RAZA del perfil: con lo que sabe de esa raza decide su tamaño típico
 * (pequeño / mediano / grande) y su pelaje típico (corto / medio / largo). Los datos del perfil
 * (tamaño en cm, peso, pelaje) son de apoyo, sobre todo para "Criollo" o razas mezcladas.
 *
 *   Duraciones:  BAJA = hasta 2 h · MEDIA = de 2 a 4 h · ALTA = más de 4 h
 *
 *                 pelaje corto   pelaje medio   pelaje largo
 *   pequeño          BAJA           BAJA           MEDIA
 *   mediano          MEDIA          MEDIA          MEDIA
 *   grande           MEDIA          ALTA           ALTA
 *
 * Si no hay API key o OpenAI falla, se usa la MISMA tabla con los datos del perfil (respaldo).
 * El resultado se guarda en caché por raza/pelaje/tamaño para no consultar la IA cada vez que el
 * cliente cambia de día en el calendario.
 */
@ApplicationScoped
public class EstimadorDuracionCitaGptAdapter implements EstimadorDuracionCitaPort {

    private static final Logger LOG = Logger.getLogger(EstimadorDuracionCitaGptAdapter.class);

    private static final String SYSTEM_PROMPT = """
            Eres el asistente de una peluquería canina y felina. Tu tarea es estimar cuánto dura
            el servicio de BAÑO Y PELUQUERÍA de una mascota.

            Paso 1. Usa tu conocimiento de la RAZA para decidir:
              - Tamaño típico de la raza: PEQUEÑO, MEDIANO o GRANDE (gigante cuenta como GRANDE).
              - Pelaje típico de la raza: CORTO, MEDIO o LARGO (doble capa o pelo que requiere corte cuenta como LARGO).
              Los datos del perfil (tamaño en cm, peso, pelaje) son de apoyo. Si la raza es "Criollo",
              mestiza o no la reconoces, guíate por los datos del perfil.

            Paso 2. Aplica esta tabla:
              PEQUEÑO + CORTO -> BAJA
              PEQUEÑO + MEDIO -> BAJA
              PEQUEÑO + LARGO -> MEDIA
              MEDIANO + CORTO -> MEDIA
              MEDIANO + MEDIO -> MEDIA
              MEDIANO + LARGO -> MEDIA
              GRANDE  + CORTO -> MEDIA
              GRANDE  + MEDIO -> ALTA
              GRANDE  + LARGO -> ALTA
              Gatos: BAJA, salvo razas de pelo largo (por ejemplo Persa, Maine Coon, Angora) -> MEDIA.

            Significado: BAJA = hasta 2 horas, MEDIA = de 2 a 4 horas, ALTA = más de 4 horas.

            Responde SOLO una palabra: BAJA, MEDIA o ALTA. Sin puntuación ni explicaciones.
            """;

    // Caché: misma raza + pelaje + tamaño -> misma duración (evita llamar a la IA en cada consulta)
    private final Map<String, DuracionCita> cache = new ConcurrentHashMap<>();

    @Inject
    @RestClient
    OpenAiClient client;

    @ConfigProperty(name = "openai.api-key")
    Optional<String> apiKey;

    @ConfigProperty(name = "openai.model")
    String model;

    @Override
    public DuracionCita estimar(Mascota mascota) {
        String clave = clave(mascota);
        DuracionCita guardada = cache.get(clave);
        if (guardada != null) {
            return guardada;
        }

        DuracionCita duracion = consultarIa(mascota);
        if (duracion == null) {
            duracion = respaldo(mascota);
        } else {
            cache.put(clave, duracion);   // solo se guarda lo que respondió la IA
        }
        return duracion;
    }

    // Devuelve null si no se pudo usar la IA (sin key, error de red o respuesta inválida)
    private DuracionCita consultarIa(Mascota mascota) {
        String key = apiKey == null ? "" : apiKey.orElse("").trim();
        if (key.isEmpty()) {
            LOG.warn("OPENAI_API_KEY no configurada; se usa la regla de respaldo (raza/pelaje/tamaño del perfil)");
            return null;
        }
        try {
            String userPrompt = """
                    Tipo de mascota: %s
                    Raza: %s
                    Pelaje registrado en el perfil: %s
                    Tamaño registrado en el perfil: %s
                    Peso registrado en el perfil: %s
                    """.formatted(
                    mascota.getTipo() == null ? "no indicado" : mascota.getTipo().name().toLowerCase(Locale.ROOT),
                    texto(mascota.getRaza()),
                    mascota.getPelaje() == null ? "no indicado" : mascota.getPelaje().name().toLowerCase(Locale.ROOT),
                    mascota.getTamano() == null ? "no indicado" : mascota.getTamano() + " cm de altura",
                    mascota.getPeso() == null ? "no indicado" : mascota.getPeso() + " kg");

            OpenAiResponse response = client.chat("Bearer " + key, OpenAiRequest.of(model, SYSTEM_PROMPT, userPrompt));
            DuracionCita duracion = parsear(response == null ? null : response.firstContent());
            if (duracion == null) {
                LOG.warn("Respuesta de GPT no reconocida; se usa la regla de respaldo");
            } else {
                LOG.infof("IA: %s (%s, pelaje %s) -> %s", mascota.getNombre(), mascota.getRaza(), mascota.getPelaje(), duracion);
            }
            return duracion;
        } catch (Exception e) {
            LOG.warn("Fallo al consultar GPT, se usa la regla de respaldo: " + e.getMessage());
            return null;
        }
    }

    // La IA a veces responde "Media." o "ALTA\n": se toma la primera palabra válida
    private DuracionCita parsear(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-ZÁÉÍÓÚ]", " ").trim();
        for (String palabra : limpio.split("\\s+")) {
            switch (palabra) {
                case "BAJA": return DuracionCita.BAJA;
                case "MEDIA": return DuracionCita.MEDIA;
                case "ALTA": return DuracionCita.ALTA;
                default: break;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- Respaldo sin IA

    private enum Tamano { PEQUENO, MEDIANO, GRANDE }

    // Misma tabla del prompt, usando los datos del perfil
    private DuracionCita respaldo(Mascota mascota) {
        Pelaje pelaje = mascota.getPelaje() == null ? Pelaje.MEDIO : mascota.getPelaje();

        if (mascota.getTipo() == TipoMascota.GATO) {
            return pelaje == Pelaje.LARGO ? DuracionCita.MEDIA : DuracionCita.BAJA;
        }

        Tamano tamano = tamano(mascota);
        return switch (tamano) {
            case PEQUENO -> pelaje == Pelaje.LARGO ? DuracionCita.MEDIA : DuracionCita.BAJA;
            case MEDIANO -> DuracionCita.MEDIA;
            case GRANDE -> pelaje == Pelaje.CORTO ? DuracionCita.MEDIA : DuracionCita.ALTA;
        };
    }

    // Tamaño por altura (cm); si no hay, por peso (kg). Valores del formulario: 25 / 45 / 60 / 75 cm
    private Tamano tamano(Mascota mascota) {
        if (mascota.getTamano() != null && mascota.getTamano() > 0) {
            float cm = mascota.getTamano();
            return cm < 35 ? Tamano.PEQUENO : cm < 55 ? Tamano.MEDIANO : Tamano.GRANDE;
        }
        if (mascota.getPeso() != null && mascota.getPeso() > 0) {
            float kg = mascota.getPeso();
            return kg < 10 ? Tamano.PEQUENO : kg < 25 ? Tamano.MEDIANO : Tamano.GRANDE;
        }
        return Tamano.MEDIANO;
    }

    private static String clave(Mascota m) {
        return String.join("|",
                String.valueOf(m.getTipo()),
                texto(m.getRaza()).toLowerCase(Locale.ROOT),
                String.valueOf(m.getPelaje()),
                String.valueOf(m.getTamano()),
                String.valueOf(m.getPeso()));
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? "no indicada" : valor.trim();
    }
}