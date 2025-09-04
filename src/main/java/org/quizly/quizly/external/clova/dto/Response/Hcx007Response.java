package org.quizly.quizly.external.clova.dto.Response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.quizly.quizly.core.domin.entity.Quiz;
import org.quizly.quizly.core.domin.entity.SolveHistory;


@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Hcx007Response {
  private String quiz;
  private Quiz.QuizType type;
  private List<String> options;
  private String answer;
  private String explanation;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String latestSolveStatus;
  private String topic;
}

