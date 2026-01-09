package org.quizly.quizly.account.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.quizly.quizly.core.domin.entity.Quiz.QuizType;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "마이페이지 대시보드 응답")
public class ReadDashboardResponse {

  @Schema(description = "오늘의 학습 통계: 총 풀이 수, 정답 수, 오답 수(오늘)")
  private TodaySummary todaySummary;

  @Schema(description = "이번 달 누적 학습 통계 (이번 달 1일 ~ 오늘)")
  private CumulativeSummary cumulativeSummary;

  @Schema(description = "문제 유형별 통계 (이번 달 1일 ~ 오늘)")
  private List<QuizTypeSummary> quizTypeSummaryList;

  @Schema(description = "주제 유형별 통계 (최근 등록된 6개)")
  private List<TopicSummary> topicSummaryList;

  @Schema(description = "월별 학습 문제 기록 - 문제를 푼 날짜만 반환")
  private List<DailySummary> dailySummaryList;

  @Schema(description = "시간대별 학습 패턴 (7개 시간대 모두 반환)")
  private List<HourlySummary> hourlySummaryList;

  @Schema(description = "AI 기반 학습 분석 결과")
  private AiAnalysis aiAnalysis;

  public record TodaySummary(
      @Schema(description = "총 풀이 수", example = "10")
      int solvedCount,
      @Schema(description = "정답 수", example = "7")
      int correctCount,
      @Schema(description = "오답 수", example = "3")
      int wrongCount
  ){}

  public record CumulativeSummary(
      @Schema(description = "총 풀이 수", example = "100")
      int solvedCount,
      @Schema(description = "정답 수", example = "75")
      int correctCount,
      @Schema(description = "오답 수", example = "25")
      int wrongCount
  ){}

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

  public record TopicSummary(
      @Schema(description = "주제 명")
      String topic,
      @Schema(description = "총 풀이 수", example = "50")
      int solvedCount,
      @Schema(description = "정답 수", example = "40")
      int correctCount,
      @Schema(description = "오답 수", example = "10")
      int wrongCount

  ){}

  public record DailySummary(
      @Schema(description = "날짜", example = "2025-12-01")
      LocalDate date,
      @Schema(description = "해당 날짜에 푼 문제 수", example = "5")
      int solvedCount
  ){}

  public record HourlySummary(
      @Schema(description = "시작 시각 (0, 6, 9, 12, 15, 18, 21)", example = "9")
      int startHour,
      @Schema(description = "해당 시간대에 푼 문제 수", example = "15")
      int solvedCount
  ){}
  public record AiAnalysis(
          @Schema(description = "AI 학습 분석 결과")
          String analysisText
  ){}
}