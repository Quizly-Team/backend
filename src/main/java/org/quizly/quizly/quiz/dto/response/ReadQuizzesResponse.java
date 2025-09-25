package org.quizly.quizly.quiz.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;

@Getter
@NoArgsConstructor
@ToString
@SuperBuilder
@Schema(description = "그룹별 문제 조회 응답")
public class ReadQuizzesResponse extends BaseResponse<GlobalErrorCode> {

  @Schema(description = "그룹별 문제")
  private List<QuizGroup> quizGroupList;

  @Getter
  @NoArgsConstructor
  @ToString
  @SuperBuilder
  @Schema(description = "문제 그룹 정보")
  public static class QuizGroup {

    @Schema(description = "그룹명", example = "자바")
    private String group;

    @Schema(description = "해당 그룹의 풀이 결과가 포함된 문제 목록")
    private List<QuizHistoryDetail> quizHistoryDetailList;
  }

  @Schema(description = "풀이 결과가 포함된 문제 상세 정보")
  public record QuizHistoryDetail(
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
      @Schema(description = "해설", example = "XP는 소규모 릴리즈를 기본 원리로 하며,dd 대규모 릴리즈는 XP의 원리에 포함되지 않는다.")
      String explanation,
      @Schema(description = "주제", example = "개발 방법론")
      String topic,
      @Schema(description = "마지막 풀이 정답 여부", example = "true")
      boolean isLastSolveCorrect
  ){}
}