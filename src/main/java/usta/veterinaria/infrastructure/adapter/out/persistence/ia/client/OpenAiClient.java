package usta.veterinaria.infrastructure.adapter.out.persistence.ia.client;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import usta.veterinaria.infrastructure.adapter.out.persistence.ia.client.dto.OpenAiRequest;
import usta.veterinaria.infrastructure.adapter.out.persistence.ia.client.dto.OpenAiResponse;

@RegisterRestClient(configKey = "openai")
@Path("/v1/chat/completions")
public interface OpenAiClient {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    OpenAiResponse chat(@HeaderParam("Authorization") String auth, OpenAiRequest request);
}