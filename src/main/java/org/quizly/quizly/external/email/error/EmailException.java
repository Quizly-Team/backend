package org.quizly.quizly.external.email.error;

import org.springframework.http.HttpStatus;

public class EmailException extends RuntimeException{

    private HttpStatus httpStatus;

    public EmailException(String message) {
        super(message);
    }

    public EmailException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }
}
