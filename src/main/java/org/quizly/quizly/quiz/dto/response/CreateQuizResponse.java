package org.quizly.quizly.quiz.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
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
@Schema(description = "문제 생성 응답")
public class CreateQuizResponse extends BaseResponse<GlobalErrorCode> {

  @Schema(description = "문제 정보")
  private List<QuizDetail> quizDetailList;

  public record QuizDetail(
      @Schema(description = "문제 ID", example = "1")
      Long quizId,
      @Schema(description = "문제", example = "XP의 기본 원리에 포함되지 않는 것은?")
      String text,
      @Schema(description = "문제 타입 MULTIPLE_CHOICE:객관식, TRUE_FALSE:OX", example = "MULTIPLE_CHOICE")
      String type,
      @Schema(description = "OX 문제는 빈 배열 반환", example = "[\n"
          + "        \"계획 절차\",\n"
          + "        \"대규모 릴리즈\",\n"
          + "        \"상징(Metaphor)\",\n"
          + "        \"지속적 통합(CI)\"\n"
          + "      ]")
      List<String> options,
      @Schema(description = "정답", example = "대규모 릴리즈")
      String answer,
      @Schema(description = "해설", example = "XP는 소규모 릴리즈를 기본 원리로 하며, 대규모 릴리즈는 XP의 원리에 포함되지 않는다.")
      String explanation,
      @Schema(description = "주제", example = "개발 방법론")
      String topic
  ){}

}
