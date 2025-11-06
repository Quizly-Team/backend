package org.quizly.quizly.external.storage.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class StorageException extends RuntimeException {
    private HttpStatus httpStatus;

    public StorageException(String message) {
        super(message);
    }

    public StorageException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }
}
