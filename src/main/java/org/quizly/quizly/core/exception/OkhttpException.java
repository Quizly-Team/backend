package org.quizly.quizly.core.exception;

import lombok.Getter;

@Getter
public class OkhttpException extends RuntimeException {

  public OkhttpException(String message) {
    super(message);
  }
}
