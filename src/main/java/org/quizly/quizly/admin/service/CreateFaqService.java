package org.quizly.quizly.admin.service;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.admin.service.CreateFaqService.CreateFaqRequest;
import org.quizly.quizly.admin.service.CreateFaqService.CreateFaqResponse;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domin.entity.Faq;
import org.quizly.quizly.core.domin.entity.Faq.FaqCategory;
import org.quizly.quizly.core.domin.repository.FaqRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateFaqService implements BaseService<CreateFaqRequest, CreateFaqResponse> {

  private final FaqRepository faqRepository;

  @Override
  public CreateFaqResponse execute(CreateFaqRequest request) {
    if (request == null || !request.isValid()) {
      return CreateFaqResponse.builder()
          .success(false)
          .errorCode(CreateFaqErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    Faq faq = Faq.builder()
        .category(request.getCategory())
        .question(request.getQuestion())
        .answer(request.getAnswer())
        .build();

    Faq savedFaq = faqRepository.save(faq);

    return CreateFaqResponse.builder()
        .faqId(savedFaq.getId())
        .build();
  }

  @Getter
  @RequiredArgsConstructor
  public enum CreateFaqErrorCode implements BaseErrorCode<DomainException> {

    NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "필수 파라미터가 누락되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public DomainException toException() {
      return new DomainException(httpStatus, this);
    }
  }

  @Getter
  @Builder
  public static class CreateFaqRequest implements BaseRequest {
    private FaqCategory category;
    private String question;
    private String answer;

    @Override
    public boolean isValid() {
      return category != null && question != null && answer != null;
    }
  }

  @Getter
  @SuperBuilder
  public static class CreateFaqResponse extends BaseResponse<CreateFaqErrorCode> {
    private Long faqId;
  }
}
