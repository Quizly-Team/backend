package org.quizly.quizly.faq.service;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
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
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadFaqService implements BaseService<ReadFaqService.ReadFaqRequest, ReadFaqService.ReadFaqResponse> {

  private final FaqRepository faqRepository;

  @Override
  public ReadFaqResponse execute(ReadFaqRequest request) {
    if (request == null) {
      log.error("[ReadFaqService] request is null - unexpected service logic error");
      return ReadFaqResponse.builder()
          .success(false)
          .errorCode(ReadFaqErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    List<Faq> faqList = request.getCategory() != null
        ? faqRepository.findByCategoryAndDeletedFalseOrderByCreatedAt(request.getCategory())
        : faqRepository.findAllByDeletedFalseOrderByCreatedAt();

    return ReadFaqResponse.builder()
        .faqList(faqList)
        .build();
  }

  @Getter
  @RequiredArgsConstructor
  public enum ReadFaqErrorCode implements BaseErrorCode<DomainException> {

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
  public static class ReadFaqRequest implements BaseRequest {

    private FaqCategory category;
  }

  @Getter
  @SuperBuilder
  public static class ReadFaqResponse extends BaseResponse<ReadFaqErrorCode> {

    private List<Faq> faqList;
  }
}
