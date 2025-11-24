package org.quizly.quizly.external.clova.dto.Request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Hcx007Request {

  private List<Message> messages;
  private Thinking thinking;
  private ResponseFormat responseFormat;

  @Builder.Default private double topP = 0.8;
  @Builder.Default private int topK = 60;
  @Builder.Default private int maxCompletionTokens = 4096;
  @Builder.Default private double temperature = 0.7;
  @Builder.Default private double repetitionPenalty = 1.0;
  @Builder.Default private int seed = ThreadLocalRandom.current().nextInt(1, 2147483647);
  @Builder.Default private boolean includeAiFilters = true;

  public static Hcx007Request of(List<Message> messages, EffortLevel effortLevel) {
    return Hcx007Request.builder()
        .messages(messages)
        .thinking(new Thinking(effortLevel))
        .build();
  }

  public static Hcx007Request of(List<Message> messages, ResponseFormat responseFormat) {
    return Hcx007Request.builder()
        .messages(messages)
        .responseFormat(responseFormat)
        .thinking(new Thinking(EffortLevel.NONE))
        .build();
  }

  @RequiredArgsConstructor
  public enum EffortLevel {
    NONE("none"),
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    private final String value;

    @JsonValue
    public String getValue() {
      return value;
    }
  }

  public record Thinking(
      EffortLevel effort
  ) {}

  public record Message(
      String role,
      List<Content> content
  ) {
    public record Content(
        String type,
        String text
    ) {}
  }

  public record ResponseFormat(
      String type,
      Object schema
  ) {
    public static ResponseFormat json(ArraySchema schema) {
      return new ResponseFormat("json", schema);
    }

    public static ResponseFormat json(ObjectSchema schema) {
      return new ResponseFormat("json", schema);
    }
  }

  // Quiz, MockExam Schema
  public record ArraySchema(
      String type,
      Items items,
      Integer minItems,
      Integer maxItems
  ) {
    public static ArraySchema of(Items items, int minItems, int maxItems) {
      return new ArraySchema("array", items, minItems, maxItems);
    }
  }

  // Topic Schema
  public record ObjectSchema(
      String type,
      List<String> required,
      Object properties
  ) {
    public static ObjectSchema of(List<String> required, Object properties) {
      return new ObjectSchema("object", required, properties);
    }
  }

  // ArraySchema 내부 데이터
  public record Items(
      String type,
      List<String> required,
      Object properties
  ) {
    public static Items of(List<String> required, Object properties) {
      return new Items("object", required, properties);
    }
  }

  public record QuizProperties(
      PropertyField quiz,
      @JsonProperty("type") TypePropertyField type,
      OptionsPropertyField options,
      PropertyField answer,
      PropertyField explanation
  ) {}

  // ObjectSchema 내부 데이터
  public record TopicProperties(
      PropertyField topic
  ) {}

  // 공통 필드 타입들
  public record PropertyField(
      String type,
      String description
  ) {}

  public record TypePropertyField(
      String type,
      String description,
      @JsonProperty("enum") List<String> enumValues
  ) {}

  public record OptionsPropertyField(
      String type,
      String description,
      ItemsInfo items,
      Integer minItems,
      Integer maxItems
  ) {
    public record ItemsInfo(
        String type
    ) {}
  }
}