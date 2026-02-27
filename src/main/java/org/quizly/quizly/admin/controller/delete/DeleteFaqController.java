package org.quizly.quizly.admin.controller.delete;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.admin.service.DeleteFaqService;
import org.quizly.quizly.admin.service.DeleteFaqService.DeleteFaqErrorCode;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자")
public class DeleteFaqController {

  private final DeleteFaqService deleteFaqService;

  @Operation(
      summary = "FAQ 삭제 API",
      description = "관리자 전용 API로 FAQ를 삭제합니다.",
      operationId = "/admin/faqs/{faqId}"
  )
  @DeleteMapping("/admin/faqs/{faqId}")
  @PreAuthorize("hasRole('ADMIN')")
  @ApiErrorCode(errorCodes = {GlobalErrorCode.class, DeleteFaqErrorCode.class})
  public ResponseEntity<Void> deleteFaq(
      @PathVariable Long faqId
  ) {
    DeleteFaqService.DeleteFaqResponse serviceResponse = deleteFaqService.execute(
        DeleteFaqService.DeleteFaqRequest.builder()
            .faqId(faqId)
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

    return ResponseEntity.noContent().build();
  }
}
