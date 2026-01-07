package org.quizly.quizly.account.service;

import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.account.service.ReadQuizTypeSummaryService.ReadQuizTypeSummaryResponse;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadCumulativeSummaryService implements BaseService<ReadCumulativeSummaryService.ReadCumulativeSummaryRequest, ReadCumulativeSummaryService.ReadCumulativeSummaryResponse> {

  @Override
  public ReadCumulativeSummaryResponse execute(ReadCumulativeSummaryRequest request) {
    if (request == null || !request.isValid()) {
      return ReadCumulativeSummaryResponse.builder()
          .success(false)
          .errorCode(ReadCumulativeSummaryErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    ReadCumulativeSummaryResponse.CumulativeSummary cumulativeSummary =
        readCumulativeSummary(request.getQuizTypeSummaryList());

    return ReadCumulativeSummaryResponse.builder()
        .cumulativeSummary(cumulativeSummary)
        .build();
  }

  private ReadCumulativeSummaryResponse.CumulativeSummary readCumulativeSummary(
      List<ReadQuizTypeSummaryResponse.QuizTypeSummary> quizTypeSummaryList) {
    return quizTypeSummaryList.stream()
        .collect(Collectors.teeing(
            Collectors.summingInt(ReadQuizTypeSummaryResponse.QuizTypeSummary::solvedCount),
            Collectors.summingInt(ReadQuizTypeSummaryResponse.QuizTypeSummary::correctCount),
            (solved, correct) -> new ReadCumulativeSummaryResponse.CumulativeSummary(solved, correct, solved - correct)
        ));
  }

  @Getter
  @RequiredArgsConstructor
  public enum ReadCumulativeSummaryErrorCode implements BaseErrorCode<DomainException> {
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
  public static class ReadCumulativeSummaryRequest implements BaseRequest {
    private List<ReadQuizTypeSummaryResponse.QuizTypeSummary> quizTypeSummaryList;

    @Override
    public boolean isValid() {
      return quizTypeSummaryList != null;
    }
  }

  @Getter
  @Setter
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class ReadCumulativeSummaryResponse extends BaseResponse<ReadCumulativeSummaryErrorCode> {
    private CumulativeSummary cumulativeSummary;

    public record CumulativeSummary(
        int solvedCount,
        int correctCount,
        int wrongCount
    ){}
  }
}
