package org.quizly.quizly.external.clova.error;

import org.springframework.http.HttpStatus;

public class ClovaException extends RuntimeException {
  private HttpStatus httpStatus;

  public ClovaException(String message) {
    super(message);
  }

  public ClovaException(HttpStatus httpStatus, String message) {
    super(message);
    this.httpStatus = httpStatus;
  }
}
