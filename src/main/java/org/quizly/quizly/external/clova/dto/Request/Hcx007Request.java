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
      Schema schema
  ) {
    public record Schema(
        String type,
        Items items,
        Integer minItems,
        Integer maxItems
    ) {
      public record Items(
          String type,
          List<String> required,
          Properties properties
      ) {
        public record Properties(
            Property quiz,
            @JsonProperty("type") TypeProperty type,
            OptionsProperty options,
            Property answer,
            Property explanation
        ) {
          public record Property(
              String type,
              String description
          ) {}

          public record TypeProperty(
              String type,
              String description,
              @JsonProperty("enum") List<String> enumValues
          ) {}

          public record OptionsProperty(
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
      }
    }
  }
}