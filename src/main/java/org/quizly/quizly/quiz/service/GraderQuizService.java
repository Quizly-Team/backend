package org.quizly.quizly.quiz.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.quiz.service.GraderQuizService.GraderQuizRequest;
import org.quizly.quizly.quiz.service.GraderQuizService.GraderQuizResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class GraderQuizService implements BaseService<GraderQuizRequest, GraderQuizResponse> {

  @Override
  public GraderQuizResponse execute(GraderQuizRequest request) {
    if (request == null || !request.isValid()) {
      return GraderQuizResponse.builder()
          .success(false)
          .errorCode(GraderQuizErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    String normalizedCorrectAnswer = normalizeText(request.getAnswer());
    String normalizedUserAnswer = normalizeText(request.getUserAnswer());

    return GraderQuizResponse.builder()
        .isCorrect(normalizedCorrectAnswer.equals(normalizedUserAnswer))
        .build();
  }

  private String normalizeText(String text) {
    if (text == null) {
      return "";
    }
    return text.trim().toUpperCase();
  }

  @Getter
  @RequiredArgsConstructor
  public enum GraderQuizErrorCode implements BaseErrorCode<DomainException> {

    NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다.");

    private final HttpStatus httpStatus;

    private final String message;

    @Override
    public DomainException toException() {
      return new DomainException(httpStatus, this);
    }
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class GraderQuizRequest implements BaseRequest {

    private String answer;

    private String userAnswer;

    @Override
    public boolean isValid() {
      return answer != null && userAnswer != null;
    }
  }

  @Getter
  @Setter
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class GraderQuizResponse extends BaseResponse<GraderQuizErrorCode> {

    private boolean isCorrect;
  }

}
