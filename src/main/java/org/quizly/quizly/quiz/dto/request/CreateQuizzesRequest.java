package org.quizly.quizly.quiz.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.domin.entity.Quiz;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "문제 생성 요청")
public class CreateQuizzesRequest implements BaseRequest {

  @Schema(description = "사용자 정리", example = "개발 방법론에는 Agile이 있으며, 대표적인 프로세스로는 XP와 Scrum이 있다.\\n\\nXP는 계획 절차, 소규모 릴리즈, 상징(Metaphor), 공동 소유, 지속적인 통합(CI)을 기본 원리로 한다.\\n\\n럼바우 방법론은 객체 모델링, 동적 모델링, 기능 모델링으로 구성되며 각각 객체 다이어그램, 상태 다이어그램, 자료 흐름도로 표현된다.")
  private String plainText;

  @Schema(description = "문제 유형", example = "MULTIPLE_CHOICE", allowableValues = {"MULTIPLE_CHOICE", "TRUE_FALSE"})
  private Quiz.QuizType type;

  @Override
  public boolean isValid() {
    return plainText != null && type != null;}

}