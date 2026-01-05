package org.quizly.quizly.account.controller.get;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.account.dto.response.ReadDashboardResponse;
import org.quizly.quizly.account.dto.response.ReadDashboardResponse.CumulativeSummary;
import org.quizly.quizly.account.dto.response.ReadDashboardResponse.DailySummary;
import org.quizly.quizly.account.dto.response.ReadDashboardResponse.QuizTypeSummary;
import org.quizly.quizly.account.service.ReadDashboardService;
import org.quizly.quizly.account.service.ReadDashboardService.ReadDashboardErrorCode;
import org.quizly.quizly.account.service.ReadDashboardService.ReadDashboardRequest;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Tag(name = "Account", description = "계정")
public class ReadDashboardController {

  private final ReadDashboardService readDashboardService;

  @Operation(
      summary = "마이페이지 대시보드 조회 API",
      description = "현재 로그인 유저의 학습 통계 대시보드 정보를 조회합니다.\n\n"
          + "- 이번 달 누적 학습 통계: 총 풀이 수, 정답 수, 오답 수(이번 달 1일 ~ 오늘)\n"
          + "- 문제 유형별 통계: 각 유형별 풀이 수, 정답 수, 오답 수(이번 달 1일 ~ 오늘)\n"
          + "- 주제 유형별 통계: 각 주제별 풀이 수, 정답 수, 오답 수(최근 6개 주제만 반환)\n"
          + "- 월별 학습 문제 기록: GitHub 잔디처럼 날짜별 풀이 수 (문제를 푼 날짜만 반환)\n",
      operationId = "/account/dashboard"
  )
  @GetMapping("/account/dashboard")
  @ApiErrorCode(errorCodes = {GlobalErrorCode.class, ReadDashboardErrorCode.class})
  public ResponseEntity<ReadDashboardResponse> readDashboard(
      @AuthenticationPrincipal UserPrincipal userPrincipal
  ) {
    ReadDashboardService.ReadDashboardServiceResponse serviceResponse =
        readDashboardService.execute(
            ReadDashboardRequest.builder()
                .userPrincipal(userPrincipal)
                .build()
        );

    if (serviceResponse == null || !serviceResponse.isSuccess()) {
      Optional.ofNullable(serviceResponse)
          .map(BaseResponse::getErrorCode)
          .ifPresentOrElse(errorCode -> {
            throw errorCode.toException();
          }, () -> {
            throw GlobalErrorCode.INTERNAL_ERROR.toException();
          });
    }
    return ResponseEntity.ok(toResponse(serviceResponse));
  }

  private ReadDashboardResponse toResponse(ReadDashboardService.ReadDashboardServiceResponse serviceResponse) {
    CumulativeSummary cumulativeSummary = new CumulativeSummary(
        serviceResponse.getCumulativeSummary().solvedCount(),
        serviceResponse.getCumulativeSummary().correctCount(),
        serviceResponse.getCumulativeSummary().wrongCount()
    );

    List<QuizTypeSummary> quizTypeSummaryList = serviceResponse.getQuizTypeSummaryList().stream()
        .map(summary -> new QuizTypeSummary(
            summary.quizType(),
            summary.solvedCount(),
            summary.correctCount(),
            summary.wrongCount()
        ))
        .collect(Collectors.toList());

    List<ReadDashboardResponse.TopicSummary> topicSummaryList = serviceResponse.getTopicSummaryList().stream()
            .map(summary -> new ReadDashboardResponse.TopicSummary(
                    summary.topic(),
                    summary.solvedCount(),
                    summary.correctCount(),
                    summary.wrongCount()
            ))
            .collect(Collectors.toList());


    List<DailySummary> dailySummaryList = serviceResponse.getDailySummaryList().stream()
        .map(summary -> new DailySummary(
            summary.date(),
            summary.solvedCount()
        ))
        .collect(Collectors.toList());

    return ReadDashboardResponse.builder()
        .quizTypeSummaryList(quizTypeSummaryList)
        .cumulativeSummary(cumulativeSummary)
        .topicSummaryList(topicSummaryList)
        .dailySummaryList(dailySummaryList)
        .build();
  }
}