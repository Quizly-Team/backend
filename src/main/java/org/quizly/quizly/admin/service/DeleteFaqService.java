package org.quizly.quizly.admin.service;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.admin.service.DeleteFaqService.DeleteFaqRequest;
import org.quizly.quizly.admin.service.DeleteFaqService.DeleteFaqResponse;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domin.entity.Faq;
import org.quizly.quizly.core.domin.repository.FaqRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class DeleteFaqService implements BaseService<DeleteFaqRequest, DeleteFaqResponse> {

  private final FaqRepository faqRepository;

  @Override
  @Transactional
  public DeleteFaqResponse execute(DeleteFaqRequest request) {
    if (request == null || !request.isValid()) {
      return DeleteFaqResponse.builder()
          .success(false)
          .errorCode(DeleteFaqErrorCode.NOT_FOUND_FAQ)
          .build();
    }

    Faq faq = faqRepository.findByIdAndDeletedFalse(request.getFaqId())
        .orElse(null);

    if (faq == null) {
      return DeleteFaqResponse.builder()
          .success(false)
          .errorCode(DeleteFaqErrorCode.NOT_FOUND_FAQ)
          .build();
    }

    faq.softDelete();
    faqRepository.save(faq);

    return DeleteFaqResponse.builder()
        .build();
  }

  @Getter
  @RequiredArgsConstructor
  public enum DeleteFaqErrorCode implements BaseErrorCode<DomainException> {

    NOT_FOUND_FAQ(HttpStatus.NOT_FOUND, "FAQ를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public DomainException toException() {
      return new DomainException(httpStatus, this);
    }
  }

  @Getter
  @Builder
  public static class DeleteFaqRequest implements BaseRequest {
    private Long faqId;

    @Override
    public boolean isValid() {
      return faqId != null;
    }
  }

  @Getter
  @SuperBuilder
  public static class DeleteFaqResponse extends BaseResponse<DeleteFaqErrorCode> {
  }
}
