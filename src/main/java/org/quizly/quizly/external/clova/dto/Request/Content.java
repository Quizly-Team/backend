package org.quizly.quizly.external.clova.dto.Request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Content {
  private String type;
  private String text;
}
