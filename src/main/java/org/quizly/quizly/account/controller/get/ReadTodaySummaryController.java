package org.quizly.quizly.account.controller.get;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.account.dto.response.ReadTodaySummaryResponse;
import org.quizly.quizly.account.dto.response.ReadTodaySummaryResponse.TodaySummary;
import org.quizly.quizly.account.service.ReadTodaySummaryService;
import org.quizly.quizly.account.service.ReadTodaySummaryService.ReadTodaySummaryErrorCode;
import org.quizly.quizly.account.service.ReadUserService;
import org.quizly.quizly.account.service.ReadUserService.ReadUserRequest;
import org.quizly.quizly.account.service.ReadUserService.ReadUserResponse;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Tag(name = "Account", description = "계정")
public class ReadTodaySummaryController {

  private final ReadTodaySummaryService readTodaySummaryService;
  private final ReadUserService readUserService;

  @Operation(
      summary = "오늘의 학습 통계 조회 API",
      description = "현재 로그인 유저의 오늘 학습 통계를 조회합니다.\n\n"
          + "- 오늘의 학습 통계: 총 풀이 수, 정답 수, 오답 수(오늘)\n",
      operationId = "/account/today-summary"
  )
  @GetMapping("/account/today-summary")
  @ApiErrorCode(errorCodes = {GlobalErrorCode.class, ReadTodaySummaryErrorCode.class})
  public ResponseEntity<ReadTodaySummaryResponse> readTodaySummary(
      @AuthenticationPrincipal UserPrincipal userPrincipal
  ) {
    ReadUserResponse readUserResponse = readUserService.execute(
        ReadUserRequest.builder()
            .userPrincipal(userPrincipal)
            .build()
    );

    if (!readUserResponse.isSuccess()) {
      Optional.ofNullable(readUserResponse.getErrorCode())
          .ifPresentOrElse(
              errorCode -> { throw errorCode.toException(); },
              () -> { throw GlobalErrorCode.INTERNAL_ERROR.toException(); }
          );
    }

    User user = readUserResponse.getUser();

    ReadTodaySummaryService.ReadTodaySummaryResponse serviceResponse =
        readTodaySummaryService.execute(
            ReadTodaySummaryService.ReadTodaySummaryRequest.builder()
                .user(user)
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

  private ReadTodaySummaryResponse toResponse(ReadTodaySummaryService.ReadTodaySummaryResponse serviceResponse) {
    TodaySummary todaySummary = new TodaySummary(
        serviceResponse.getTodaySummary().solvedCount(),
        serviceResponse.getTodaySummary().correctCount(),
        serviceResponse.getTodaySummary().wrongCount()
    );

    return ReadTodaySummaryResponse.builder()
        .todaySummary(todaySummary)
        .build();
  }
}