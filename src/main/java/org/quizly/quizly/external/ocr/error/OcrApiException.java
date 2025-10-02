package org.quizly.quizly.external.ocr.error;

import org.springframework.http.HttpStatus;

public class OcrApiException extends RuntimeException {
    private final HttpStatus httpStatus;

    public OcrApiException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
