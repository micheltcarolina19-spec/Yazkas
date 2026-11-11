package usta.veterinaria.infrastructure.adapter.out.persistence.ia.client.dto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenAiRequest(
        String model,
        List<Message> messages,
        Double temperature,
        @JsonProperty("max_completion_tokens") Integer maxCompletionTokens
) {
    public record Message(String role, String content) {}

    public static OpenAiRequest of(String model, String systemPrompt, String userPrompt) {
        return new OpenAiRequest(
                model,
                List.of(new Message("system", systemPrompt),
                        new Message("user", userPrompt)),
                0.0,
                10
        );
    }
}