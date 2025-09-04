package org.quizly.quizly.core.util.okhttp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OkHttpJsonRequest extends OkHttpRequest {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final Object request;

  public OkHttpJsonRequest(Object request) {
    this.request = request;
  }

  @Override
  public String convertRequestToString() {
    try {
      return objectMapper.writeValueAsString(this.request);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}