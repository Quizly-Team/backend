package org.quizly.quizly.account.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "오늘의 학습 통계 응답")
public class ReadTodaySummaryResponse {

  @Schema(description = "오늘의 학습 통계")
  private TodaySummary todaySummary;

  public record TodaySummary(
      @Schema(description = "총 풀이 수", example = "10")
      int solvedCount,
      @Schema(description = "정답 수", example = "7")
      int correctCount,
      @Schema(description = "오답 수", example = "3")
      int wrongCount
  ){}
}