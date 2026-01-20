package org.quizly.quizly.external.openai.error;

import org.springframework.http.HttpStatus;

public class OpenAiException extends RuntimeException{

    private HttpStatus httpStatus;

    public OpenAiException(String message){ super(message);}

    public OpenAiException(HttpStatus httpStatus, String message){
        super(message);
        this.httpStatus = httpStatus;
    }
}
