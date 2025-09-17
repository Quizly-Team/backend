package org.quizly.quizly.quiz.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.quizly.quizly.core.application.BaseRequest;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "퀴즈 채점 요청")
public class GradeQuizzesRequest implements BaseRequest {

  @Schema(description = "사용자 답변 ", example = "대규모 릴리즈")
  private String userAnswer;

  @Override
  public boolean isValid() {
    return userAnswer != null && !userAnswer.isEmpty();
  }
}
