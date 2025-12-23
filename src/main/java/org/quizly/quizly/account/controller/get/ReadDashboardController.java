package org.quizly.quizly.account.controller.get;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.account.dto.response.ReadDashboardResponse;
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
          + "- 문제 유형별 통계: 각 유형별 풀이 수, 정답 수, 오답 수(이번 달 1일 ~ 오늘)\n",
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
    List<QuizTypeSummary> quizTypeSummaryList = serviceResponse.getQuizTypeSummaryList().stream()
        .map(summary -> new QuizTypeSummary(
            summary.quizType(),
            summary.solvedCount(),
            summary.correctCount(),
            summary.wrongCount()
        ))
        .collect(Collectors.toList());

    return ReadDashboardResponse.builder()
        .quizTypeSummaryList(quizTypeSummaryList)
        .build();
  }
}