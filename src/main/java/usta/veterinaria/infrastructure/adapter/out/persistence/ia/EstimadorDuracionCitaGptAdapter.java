package usta.veterinaria.infrastructure.adapter.out.persistence.ia;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import usta.veterinaria.application.port.out.usuario.EstimadorDuracionCitaPort;
import usta.veterinaria.infrastructure.adapter.out.persistence.ia.client.OpenAiClient;
import usta.veterinaria.infrastructure.adapter.out.persistence.ia.client.dto.OpenAiRequest;
import usta.veterinaria.infrastructure.adapter.out.persistence.ia.client.dto.OpenAiResponse;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.domain.model.usuario.DuracionCita;

import java.util.Optional;

@ApplicationScoped
public class EstimadorDuracionCitaGptAdapter implements EstimadorDuracionCitaPort {

    private static final Logger LOG = Logger.getLogger(EstimadorDuracionCitaGptAdapter.class);

    private static final String SYSTEM_PROMPT = """
            Eres un asistente de una peluquería canina. Estimas la duración de una cita
            según la raza, el pelaje y el tamaño de la mascota.
            Responde SOLO una palabra: BAJA, MEDIA o ALTA. Sin puntuación ni explicaciones.
            """;

    @Inject
    @RestClient
    OpenAiClient client;

    @ConfigProperty(name = "openai.api-key")
    Optional<String> apiKey;

    @ConfigProperty(name = "openai.model")
    String model;

    @Override
    public DuracionCita estimar(Mascota mascota) {

        if (apiKey == null || apiKey.isEmpty() || apiKey.get().isBlank()) {
            LOG.warn("OPENAI_API_KEY no configurada; se usa MEDIA por defecto");
            return DuracionCita.MEDIA;
        }

        try {
            String userPrompt = """
                    Raza: %s
                    Pelaje: %s
                    Tamaño: %s
                    """.formatted(
                    mascota.getRaza(),
                    mascota.getPelaje(),
                    mascota.getTamano());

            OpenAiResponse response = client.chat(
                    "Bearer " + apiKey,
                    OpenAiRequest.of(model, SYSTEM_PROMPT, userPrompt));

            return parsear(response.firstContent());

        } catch (Exception e) {
            LOG.warn("Fallo al consultar GPT, se usa MEDIA por defecto: " + e.getMessage());
            return DuracionCita.MEDIA;
        }
    }

    private DuracionCita parsear(String texto) {
        if (texto == null) {
            return DuracionCita.MEDIA;
        }
        return switch (texto.trim().toUpperCase()) {
            case "BAJA"  -> DuracionCita.BAJA;
            case "MEDIA" -> DuracionCita.MEDIA;
            case "ALTA"  -> DuracionCita.ALTA;
            default      -> DuracionCita.MEDIA;
        };
    }
}