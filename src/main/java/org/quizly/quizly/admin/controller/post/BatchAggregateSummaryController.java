package org.quizly.quizly.admin.controller.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.admin.dto.request.BatchAggregateSummaryRequest;
import org.quizly.quizly.admin.dto.response.BatchAggregateSummaryResponse;
import org.quizly.quizly.admin.service.BatchAggregateSummaryService;
import org.quizly.quizly.admin.service.BatchAggregateSummaryService.BatchAggregateSummaryErrorCode;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자")
public class BatchAggregateSummaryController {

  private final BatchAggregateSummaryService batchAggregateSummaryService;

  @Operation(
      summary = "일별 통계 배치 수동 실행 API",
      description = "지정한 날짜의 일별 유저 통계를 수동으로 집계합니다.\n\n"
          + "- 미래 날짜는 처리 불가 (당일 이전까지 가능)\n"
          + "- 해당 날짜의 통계만 집계",
      operationId = "/admin/batch/aggregate-summary"
  )
  @PostMapping("/admin/batch/aggregate-summary")
  @PreAuthorize("hasRole('ADMIN')")
  @ApiErrorCode(errorCodes = {GlobalErrorCode.class, BatchAggregateSummaryErrorCode.class})
  public ResponseEntity<BatchAggregateSummaryResponse> batchAggregateSummary(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @Valid @RequestBody BatchAggregateSummaryRequest request
  ) {
    BatchAggregateSummaryService.BatchAggregateSummaryResponse serviceResponse = batchAggregateSummaryService.execute(
        BatchAggregateSummaryService.BatchAggregateSummaryRequest.builder()
            .userPrincipal(userPrincipal)
            .targetDate(request.getTargetDate())
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

    return ResponseEntity.ok(BatchAggregateSummaryResponse.builder().build());
  }
}