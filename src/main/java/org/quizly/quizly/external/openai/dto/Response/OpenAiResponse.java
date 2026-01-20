package org.quizly.quizly.external.openai.dto.Response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class OpenAiResponse {

    private List<Choice> choices;

    public record Choice(
        Message message
    ) {
        public record Message(
            String role,
            String content
        ) {}
    }

    public String extractText() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }

        Choice firstChoice = choices.get(0);
        if (firstChoice.message() == null) {
            return null;
        }

        return firstChoice.message().content();
    }
}
