package org.quizly.quizly.account.service;

import java.util.stream.Collectors;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.account.service.ReadDashboardService.ReadDashboardServiceResponse.CumulativeSummary;
import org.quizly.quizly.account.service.ReadDashboardService.ReadDashboardServiceResponse.QuizTypeSummary;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domin.entity.Quiz.QuizType;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.repository.SolveHistoryRepository;
import org.quizly.quizly.core.domin.repository.UserQuizTypeDailySummaryRepository;
import org.quizly.quizly.core.domin.repository.UserRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadDashboardService implements BaseService<ReadDashboardService.ReadDashboardRequest, ReadDashboardService.ReadDashboardServiceResponse> {

  private final UserRepository userRepository;
  private final SolveHistoryRepository solveHistoryRepository;
  private final UserQuizTypeDailySummaryRepository userQuizTypeDailySummaryRepository;

  @Override
  public ReadDashboardServiceResponse execute(ReadDashboardRequest request) {
    if (request == null || !request.isValid()) {
      return ReadDashboardServiceResponse.builder()
          .success(false)
          .errorCode(ReadDashboardErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    String providerId = request.getUserPrincipal().getProviderId();
    if (providerId == null || providerId.isBlank()) {
      return ReadDashboardServiceResponse.builder()
          .success(false)
          .errorCode(ReadDashboardErrorCode.NOT_EXIST_PROVIDER_ID)
          .build();
    }

    Optional<User> userOptional = userRepository.findByProviderId(providerId);
    if (userOptional.isEmpty()) {
      log.error("[ReadDashboardService] User not found for providerId: {}", providerId);
      return ReadDashboardServiceResponse.builder()
          .success(false)
          .errorCode(ReadDashboardErrorCode.NOT_FOUND_USER)
          .build();
    }
    User user = userOptional.get();

    List<QuizTypeSummary> quizTypeSummaryList = calculateQuizTypeSummaryList(user);
    CumulativeSummary cumulativeSummary = calculateCumulativeSummary(quizTypeSummaryList);

    return ReadDashboardServiceResponse.builder()
        .quizTypeSummaryList(quizTypeSummaryList)
        .cumulativeSummary(cumulativeSummary)
        .build();
  }

  private List<ReadDashboardServiceResponse.QuizTypeSummary> calculateQuizTypeSummaryList(User user) {
    LocalDate today = LocalDate.now();

    Map<QuizType, QuizTypeCounts> aggregateCountMap = new EnumMap<>(QuizType.class);

    aggregatePastSummaryList(user, today, aggregateCountMap);
    aggregateTodaySolveHistoryList(user, today, aggregateCountMap);

    return toQuizTypeSummaryList(aggregateCountMap);
  }

  private void aggregatePastSummaryList(User user, LocalDate today, Map<QuizType, QuizTypeCounts> aggregateCountMap) {
    LocalDate startOfMonth = today.withDayOfMonth(1);
    LocalDate yesterday = today.minusDays(1);

    if (startOfMonth.isAfter(yesterday)) {
      return;
    }

    userQuizTypeDailySummaryRepository
        .findByUserAndDateBetween(user, startOfMonth, yesterday)
        .forEach(summary ->
            aggregateCountMap
                .computeIfAbsent(summary.getQuizType(), k -> new QuizTypeCounts())
                .add(summary.getSolvedCount(), summary.getCorrectCount())
        );
  }

  private void aggregateTodaySolveHistoryList(User user, LocalDate today, Map<QuizType, QuizTypeCounts> aggregateCountMap) {
    solveHistoryRepository
        .findFirstAttemptsByQuizTypeAndDate(user, today)
        .forEach(summary -> {
          int solved = Optional.ofNullable(summary.getTotalCount()).map(Long::intValue).orElse(0);
          int correct = Optional.ofNullable(summary.getCorrectCount()).map(Long::intValue).orElse(0);

          aggregateCountMap.computeIfAbsent(summary.getQuizType(), k -> new QuizTypeCounts())
              .add(solved, correct);
        });
  }

  private List<ReadDashboardServiceResponse.QuizTypeSummary> toQuizTypeSummaryList(Map<QuizType, QuizTypeCounts> aggregateCountMap) {
    return aggregateCountMap.entrySet().stream()
        .map(entry -> new ReadDashboardServiceResponse.QuizTypeSummary(
            entry.getKey(),
            entry.getValue().getSolvedCount(),
            entry.getValue().getCorrectCount(),
            entry.getValue().getWrongCount()
        ))
        .toList();
  }

  private ReadDashboardServiceResponse.CumulativeSummary calculateCumulativeSummary(
      List<ReadDashboardServiceResponse.QuizTypeSummary> quizTypeSummaryList) {
    return quizTypeSummaryList.stream()
        .collect(Collectors.teeing(
            Collectors.summingInt(ReadDashboardServiceResponse.QuizTypeSummary::solvedCount),
            Collectors.summingInt(ReadDashboardServiceResponse.QuizTypeSummary::correctCount),
            (solved, correct) -> new ReadDashboardServiceResponse.CumulativeSummary(solved, correct, solved - correct)
        ));
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
  public enum ReadDashboardErrorCode implements BaseErrorCode<DomainException> {
    NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
    NOT_EXIST_PROVIDER_ID(HttpStatus.BAD_REQUEST, "Provider ID가 존재하지 않습니다."),
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다.");

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
  public static class ReadDashboardRequest implements BaseRequest {
    private UserPrincipal userPrincipal;

    @Override
    public boolean isValid() {
      return userPrincipal != null;
    }
  }

  @Getter
  @Setter
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class ReadDashboardServiceResponse extends BaseResponse<ReadDashboardErrorCode> {
    private CumulativeSummary cumulativeSummary;
    private List<QuizTypeSummary> quizTypeSummaryList;

    public record CumulativeSummary(
        int solvedCount,
        int correctCount,
        int wrongCount
    ){}

    public record QuizTypeSummary(
        QuizType quizType,
        int solvedCount,
        int correctCount,
        int wrongCount
    ){}
  }
}