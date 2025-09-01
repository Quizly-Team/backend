package org.quizly.quizly.core.exception;

import java.util.Optional;
import org.quizly.quizly.core.presentation.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class SystemExceptionHandler {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception e) {
    log.error("[SystemExceptionHandler] handleException", e);
    return ResponseEntity.internalServerError()
        .body(ErrorResponse.of(500, e));
  }

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ErrorResponse> handleException(DomainException e) {
    log.error("[SystemExceptionHandler] handleDomainException", e);
    HttpStatus httpStatus = Optional.ofNullable(e.getHttpStatus()).orElse(HttpStatus.INTERNAL_SERVER_ERROR);
    ErrorResponse errorResponse = ErrorResponse.of(e);
    return ResponseEntity.status(httpStatus)
        .body(errorResponse);
  }
}
