package org.quizly.quizly.external.gemini.error;

import org.springframework.http.HttpStatus;

public class GeminiException extends RuntimeException {

    private HttpStatus httpStatus;

    public GeminiException(String message) {
        super(message);
    }

    public GeminiException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }
}
