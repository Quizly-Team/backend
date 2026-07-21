package org.quizly.quizly.core.exception;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.quizly.quizly.core.presentation.ErrorResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class SystemExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException e) {
        HttpStatus httpStatus = Optional.ofNullable(e.getHttpStatus())
            .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
        if (httpStatus.is5xxServerError()) {
            log.error("[SystemExceptionHandler] handleDomainException", e);
        } else {
            log.warn("[SystemExceptionHandler] 비즈니스 예외: {}", e.getMessage());
        }
        return ResponseEntity.status(httpStatus)
            .body(ErrorResponse.of(e));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("[SystemExceptionHandler] Access Denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse.of(403, e));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e) {
        log.error("[SystemExceptionHandler] handleException", e);
        return ResponseEntity.internalServerError()
            .body(ErrorResponse.of(500, e));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
        HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is5xxServerError()) {
            log.error("[SystemExceptionHandler] 프레임워크 서버 오류", ex);
        } else {
            log.warn("[SystemExceptionHandler] 프레임워크 요청 오류({}): {}",
                statusCode.value(), ex.getMessage());
        }
        ErrorResponse errorResponse = ErrorResponse.of(statusCode.value(), ex);
        return new ResponseEntity<>(errorResponse, headers, statusCode);
    }
}
