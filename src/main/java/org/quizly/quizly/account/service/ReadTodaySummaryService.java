package org.quizly.quizly.account.service;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.account.service.ReadTodaySummaryService.ReadTodaySummaryRequest;
import org.quizly.quizly.account.service.ReadTodaySummaryService.ReadTodaySummaryResponse;
import org.quizly.quizly.account.service.ReadTodaySummaryService.ReadTodaySummaryResponse.TodaySummary;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.repository.SolveHistoryRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadTodaySummaryService  implements BaseService<ReadTodaySummaryRequest, ReadTodaySummaryResponse> {

  private final SolveHistoryRepository solveHistoryRepository;

  @Override
  public ReadTodaySummaryResponse execute(ReadTodaySummaryRequest request) {
    if (request == null || !request.isValid()) {
      return ReadTodaySummaryResponse.builder()
          .success(false)
          .errorCode(ReadTodaySummaryErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    User user = request.getUser();
    LocalDate today = LocalDate.now();
    TodaySummary todaySummary = getTodaySummary(user, today);

    return ReadTodaySummaryResponse.builder()
        .todaySummary(todaySummary)
        .build();
  }

  private TodaySummary getTodaySummary(User user, LocalDate today) {
    List<SolveHistoryRepository.QuizTypeSummary> quizTypeSummaryList =
        solveHistoryRepository.findFirstAttemptsByQuizTypeAndDate(user, today);

    int totalSolved = 0;
    int totalCorrect = 0;

    for (SolveHistoryRepository.QuizTypeSummary quizTypeSummary : quizTypeSummaryList) {
      totalSolved += Optional.ofNullable(quizTypeSummary.getTotalCount()).map(Long::intValue).orElse(0);
      totalCorrect += Optional.ofNullable(quizTypeSummary.getCorrectCount()).map(Long::intValue).orElse(0);
    }

    int totalWrong = totalSolved - totalCorrect;

    return new TodaySummary(totalSolved, totalCorrect, totalWrong);
  }

  @Getter
  @RequiredArgsConstructor
  public enum ReadTodaySummaryErrorCode implements BaseErrorCode<DomainException> {
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
  public static class ReadTodaySummaryRequest implements BaseRequest {
    private User user;

    @Override
    public boolean isValid() {
      return user != null;
    }
  }

  @Getter
  @Setter
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class ReadTodaySummaryResponse extends
      BaseResponse<ReadTodaySummaryErrorCode> {
    private TodaySummary todaySummary;

    public record TodaySummary(
        int solvedCount,
        int correctCount,
        int wrongCount
    ){}
  }

}
