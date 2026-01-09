package org.quizly.quizly.external.gemini.dto.Request;

import java.util.List;

public record GeminiRequest (
    List<Content> contents
){
    public static GeminiRequest from(String fullPrompt) {
        return new GeminiRequest(
            List.of(
                new Content(
                    "user",
                    List.of(new Part(fullPrompt))
                )
            )
        );
    }



    public record Content(
        String role,
        List<Part> parts
    ){}

    public record Part(
        String text
    ){}


}

