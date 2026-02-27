package org.quizly.quizly.faq.controller.get;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.faq.dto.response.ReadFaqResponse;
import org.quizly.quizly.faq.dto.response.ReadFaqResponse.FaqCategoryGroup;
import org.quizly.quizly.faq.dto.response.ReadFaqResponse.FaqDetail;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.quizly.quizly.core.domin.entity.Faq;
import org.quizly.quizly.core.domin.entity.Faq.FaqCategory;
import org.quizly.quizly.faq.service.ReadFaqService;
import org.quizly.quizly.faq.service.ReadFaqService.ReadFaqErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "FAQ", description = "FAQ")
public class ReadFaqController {

  private final ReadFaqService readFaqService;

  @Operation(
      summary = "FAQ 조회 API",
      description = "FAQ 목록을 카테고리별로 조회합니다.\n\n"
          + "- `category` 미입력 시 전체 반환\n"
          + "- 카테고리: SERVICE_INTRO(서비스 소개), QUIZ_GENERATION(문제 생성), WRONG_ANSWER(오답 관리), TECH_SUPPORT(기술 지원)",
      operationId = "/faqs"
  )
  @GetMapping("/faqs")
  @ApiErrorCode(errorCodes = {GlobalErrorCode.class, ReadFaqErrorCode.class})
  public ResponseEntity<ReadFaqResponse> readFaqs(
      @RequestParam(required = false) FaqCategory category
  ) {
    ReadFaqService.ReadFaqResponse serviceResponse = readFaqService.execute(
        ReadFaqService.ReadFaqRequest.builder()
            .category(category)
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

  private ReadFaqResponse toResponse(ReadFaqService.ReadFaqResponse serviceResponse) {
    Map<FaqCategory, List<Faq>> groupedMap = serviceResponse.getFaqList().stream()
        .collect(Collectors.groupingBy(Faq::getCategory));

    List<FaqCategoryGroup> faqCategoryGroupList = Arrays.stream(FaqCategory.values())
        .filter(groupedMap::containsKey)
        .map(category -> new FaqCategoryGroup(
            category,
            category.getDescription(),
            groupedMap.get(category).stream()
                .map(faq -> new FaqDetail(faq.getId(), faq.getQuestion(), faq.getAnswer()))
                .toList()
        ))
        .toList();

    return ReadFaqResponse.builder()
        .faqCategoryGroupList(faqCategoryGroupList)
        .build();
  }
}
