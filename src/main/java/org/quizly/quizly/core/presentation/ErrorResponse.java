package org.quizly.quizly.core.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Getter;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.core.util.TimeUtil;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

  private final int statusCode;

  private final String timeStamp;

  private final boolean ok;

  private final Error error;

  private final String code;

  @Getter
  @Builder
  public static class Error {

    private final String message;
  }


  public static ErrorResponse of(int statusCode, Exception exception) {
    return ErrorResponse.builder()
        .statusCode(statusCode)
        .timeStamp(TimeUtil.toString(ZonedDateTime.now()))
        .code(exception.getClass().getSimpleName())
        .error(Error.builder().message(exception.getMessage()).build())
        .build();
  }

  public static ErrorResponse of(BaseErrorCode baseErrorCode) {
    return ErrorResponse.builder()
        .statusCode(baseErrorCode.getHttpStatus().value())
        .timeStamp(TimeUtil.toString(ZonedDateTime.now()))
        .code(baseErrorCode.name())
        .error(Error.builder().message(baseErrorCode.getMessage()).build())
        .build();
  }

  public static ErrorResponse of(DomainException exception) {
    return ErrorResponse.builder()
        .statusCode(exception.getHttpStatus().value())
        .timeStamp(TimeUtil.toString(ZonedDateTime.now(ZoneOffset.UTC)))
        .code(exception.getCode())
        .error(Error.builder().message(exception.getMessage()).build())
        .build();
  }
}

