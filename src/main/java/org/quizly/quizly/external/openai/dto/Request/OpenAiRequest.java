package org.quizly.quizly.external.openai.dto.Request;

import java.util.List;

public record OpenAiRequest(
    String model,
    List<Message> messages,
    double temperature
) {

    public static OpenAiRequest from(
        String systemPrompt,
        String userInput,
        String model,
        double temperature
    ) {
        return new OpenAiRequest(
            model,
            List.of(
                Message.system(systemPrompt),
                Message.user(userInput)
            ),
            temperature
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
}
