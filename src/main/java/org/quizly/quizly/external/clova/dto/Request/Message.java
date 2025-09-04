package org.quizly.quizly.external.clova.dto.Request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Message {
  private String role;
  private List<Content> content;
}