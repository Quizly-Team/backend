package org.quizly.quizly.account.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.quizly.quizly.core.domin.entity.Quiz.QuizType;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "마이페이지 대시보드 응답")
public class ReadDashboardResponse {

  @Schema(description = "문제 유형별 통계 (최근 1달)")
  private List<QuizTypeSummary> quizTypeSummaryList;

  public record QuizTypeSummary(
      @Schema(description = "문제 유형")
      QuizType quizType,
      @Schema(description = "총 풀이 수", example = "50")
      int solvedCount,
      @Schema(description = "정답 수", example = "40")
      int correctCount,
      @Schema(description = "오답 수", example = "10")
      int wrongCount
  ){}
}