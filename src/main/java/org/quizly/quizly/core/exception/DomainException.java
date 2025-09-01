package org.quizly.quizly.core.exception;

import lombok.Getter;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
public class DomainException extends RuntimeException {

  private HttpStatus httpStatus;

  private String code;

  public DomainException(HttpStatus httpStatus, BaseErrorCode<?> errorCode) {
    super(errorCode.getMessage());
    this.httpStatus = httpStatus;
    this.code = errorCode.name();
  }
}

