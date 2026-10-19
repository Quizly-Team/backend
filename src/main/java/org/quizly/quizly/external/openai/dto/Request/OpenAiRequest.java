package org.quizly.quizly.external.openai.dto.Request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenAiRequest(
    String model,
    List<Message> messages,
    @JsonProperty("response_format") ResponseFormat responseFormat,
    @JsonProperty("reasoning_effort") String reasoningEffort
) {

    public static OpenAiRequest of(
        String model,
        String systemPrompt,
        String userInput,
        ResponseFormat responseFormat,
        String reasoningEffort
    ) {
        return new OpenAiRequest(
            model,
            List.of(
                Message.system(systemPrompt),
                Message.user(userInput)
            ),
            responseFormat,
            reasoningEffort
        );
    }

    public record Message(
        String role,
        List<Content> content
    ) {

        public static Message system(String text) {
            return new Message(
                "system",
                List.of(Content.text(text))
            );
        }

        public static Message user(String text) {
            return new Message(
                "user",
                List.of(Content.text(text))
            );
        }
    }

    public record Content(
        String type,
        String text
    ) {

        public static Content text(String text) {
            return new Content("text", text);
        }
    }

    public record ResponseFormat(
        String type,
        @JsonProperty("json_schema") JsonSchema jsonSchema
    ) {

        public static ResponseFormat jsonSchema(JsonSchema jsonSchema) {
            return new ResponseFormat("json_schema", jsonSchema);
        }
    }

    public record JsonSchema(
        String name,
        Object schema,
        boolean strict
    ) {

        public static JsonSchema of(String name, Object schema) {
            return new JsonSchema(name, schema, true);
        }
    }
}
