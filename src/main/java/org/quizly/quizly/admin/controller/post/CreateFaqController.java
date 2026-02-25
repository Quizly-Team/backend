package org.quizly.quizly.admin.controller.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.admin.dto.request.CreateFaqRequest;
import org.quizly.quizly.admin.dto.response.CreateFaqResponse;
import org.quizly.quizly.admin.service.CreateFaqService;
import org.quizly.quizly.admin.service.CreateFaqService.CreateFaqErrorCode;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자")
public class CreateFaqController {

  private final CreateFaqService createFaqService;

  @Operation(
      summary = "FAQ 등록 API",
      description = "관리자 전용 API로 FAQ를 등록합니다.\n\n"
          + "- 카테고리: SERVICE_INTRO(서비스 소개), QUIZ_GENERATION(문제 생성), WRONG_ANSWER(오답 관리), TECH_SUPPORT(기술 지원)",
      operationId = "/admin/faqs"
  )
  @PostMapping("/admin/faqs")
  @PreAuthorize("hasRole('ADMIN')")
  @ApiErrorCode(errorCodes = {GlobalErrorCode.class, CreateFaqErrorCode.class})
  public ResponseEntity<CreateFaqResponse> createFaq(
      @Valid @RequestBody CreateFaqRequest request
  ) {
    CreateFaqService.CreateFaqResponse serviceResponse = createFaqService.execute(
        CreateFaqService.CreateFaqRequest.builder()
            .category(request.getCategory())
            .question(request.getQuestion())
            .answer(request.getAnswer())
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

    return ResponseEntity.status(HttpStatus.CREATED).body(
        CreateFaqResponse.builder()
            .faqId(serviceResponse.getFaqId())
            .category(request.getCategory())
            .question(request.getQuestion())
            .answer(request.getAnswer())
            .build()
    );
  }
}
