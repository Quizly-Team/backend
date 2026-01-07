package org.quizly.quizly.account.service;

import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.account.service.ReadQuizTypeSummaryService.ReadQuizTypeSummaryResponse.QuizTypeSummary;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domin.entity.Quiz.QuizType;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.repository.SolveHistoryRepository;
import org.quizly.quizly.core.domin.repository.SolveTypeSummaryRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadQuizTypeSummaryService implements BaseService<ReadQuizTypeSummaryService.ReadQuizTypeSummaryRequest, ReadQuizTypeSummaryService.ReadQuizTypeSummaryResponse> {

  private final SolveTypeSummaryRepository solveTypeSummaryRepository;
  private final SolveHistoryRepository solveHistoryRepository;

  @Override
  public ReadQuizTypeSummaryResponse execute(ReadQuizTypeSummaryRequest request) {
    if (request == null || !request.isValid()) {
      return ReadQuizTypeSummaryResponse.builder()
          .success(false)
          .errorCode(ReadQuizTypeSummaryErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    User user = request.getUser();
    List<QuizTypeSummary> quizTypeSummaryList = readQuizTypeSummaryList(user);

    return ReadQuizTypeSummaryResponse.builder()
        .quizTypeSummaryList(quizTypeSummaryList)
        .build();
  }

  private List<QuizTypeSummary> readQuizTypeSummaryList(User user) {
    LocalDate today = LocalDate.now();

    Map<QuizType, QuizTypeCounts> pastMap = getPastSummaryMap(user, today);
    Map<QuizType, QuizTypeCounts> todayMap = getTodaySummaryMap(user, today);

    Map<QuizType, QuizTypeCounts> aggregateCountMap = new EnumMap<>(QuizType.class);
    aggregateCountMap.putAll(pastMap);
    todayMap.forEach((type, counts) ->
        aggregateCountMap.computeIfAbsent(type, k -> new QuizTypeCounts())
            .add(counts.getSolvedCount(), counts.getCorrectCount())
    );

    return aggregateCountMap.entrySet().stream()
        .map(entry -> new QuizTypeSummary(
            entry.getKey(),
            entry.getValue().getSolvedCount(),
            entry.getValue().getCorrectCount(),
            entry.getValue().getWrongCount()
        ))
        .toList();
  }

  private Map<QuizType, QuizTypeCounts> getPastSummaryMap(User user, LocalDate today) {
    LocalDate startOfMonth = today.withDayOfMonth(1);
    LocalDate yesterday = today.minusDays(1);

    Map<QuizType, QuizTypeCounts> map = new EnumMap<>(QuizType.class);

    if (startOfMonth.isAfter(yesterday)) {
      return map;
    }

    solveTypeSummaryRepository
        .findByUserAndDateBetween(user, startOfMonth, yesterday)
        .forEach(summary ->
            map.computeIfAbsent(summary.getQuizType(), k -> new QuizTypeCounts())
                .add(summary.getSolvedCount(), summary.getCorrectCount())
        );

    return map;
  }

  private Map<QuizType, QuizTypeCounts> getTodaySummaryMap(User user, LocalDate today) {
    Map<QuizType, QuizTypeCounts> map = new EnumMap<>(QuizType.class);

    solveHistoryRepository
        .findFirstAttemptsByQuizTypeAndDate(user, today)
        .forEach(summary -> {
          int solved = Optional.ofNullable(summary.getTotalCount()).map(Long::intValue).orElse(0);
          int correct = Optional.ofNullable(summary.getCorrectCount()).map(Long::intValue).orElse(0);

          map.computeIfAbsent(summary.getQuizType(), k -> new QuizTypeCounts())
              .add(solved, correct);
        });

    return map;
  }

  @Getter
  private static class QuizTypeCounts {
    private int solvedCount = 0;
    private int correctCount = 0;

    void add(int solved, int correct) {
      this.solvedCount += solved;
      this.correctCount += correct;
    }

    int getWrongCount() {
      return solvedCount - correctCount;
    }
  }

  @Getter
  @RequiredArgsConstructor
  public enum ReadQuizTypeSummaryErrorCode implements BaseErrorCode<DomainException> {
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
  public static class ReadQuizTypeSummaryRequest implements BaseRequest {
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
  public static class ReadQuizTypeSummaryResponse extends BaseResponse<ReadQuizTypeSummaryErrorCode> {
    private List<QuizTypeSummary> quizTypeSummaryList;

    public record QuizTypeSummary(
        QuizType quizType,
        int solvedCount,
        int correctCount,
        int wrongCount
    ){}
  }
}
