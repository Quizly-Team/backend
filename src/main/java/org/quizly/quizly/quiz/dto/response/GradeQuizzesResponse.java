package org.quizly.quizly.quiz.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Schema(description = "문제 채점 응답")
public class GradeQuizzesResponse extends BaseResponse<GlobalErrorCode> {

  @Schema(description = "문제 ID", example = "1")
  private Long quizId;

  @Schema(description = "정답 여부", example = "TRUE")
  private boolean isCorrect;

  @Schema(description = "정답", example = "대규모 릴리즈")
  private String answer;

  @Schema(description = "해설", example =  "XP는 소규모 릴리즈를 기본 원리로 하며, 대규모 릴리즈는 XP의 원리에 포함되지 않는다.")
  private String explanation;
}
