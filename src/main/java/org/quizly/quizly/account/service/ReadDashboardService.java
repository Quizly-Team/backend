package org.quizly.quizly.account.service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.account.service.ReadDashboardService.ReadDashboardServiceResponse.CumulativeSummary;
import org.quizly.quizly.account.service.ReadDashboardService.ReadDashboardServiceResponse.DailySummary;
import org.quizly.quizly.account.service.ReadDashboardService.ReadDashboardServiceResponse.QuizTypeSummary;
import org.quizly.quizly.account.service.ReadDashboardService.ReadDashboardServiceResponse.HourlySummary;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domin.entity.Quiz.QuizType;
import org.quizly.quizly.core.domin.entity.SolveHourlySummary;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.repository.SolveHistoryRepository;
import org.quizly.quizly.core.domin.repository.SolveHourlySummaryRepository;
import org.quizly.quizly.core.domin.repository.UserQuizTypeDailySummaryRepository;
import org.quizly.quizly.core.domin.repository.UserRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.data.domain.PageRequest;
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

  private static final int[] TIME_SLOTS = {0, 6, 9, 12, 15, 18, 21};

  private final UserRepository userRepository;
  private final SolveHistoryRepository solveHistoryRepository;
  private final UserQuizTypeDailySummaryRepository userQuizTypeDailySummaryRepository;
  private final SolveHourlySummaryRepository solveHourlySummaryRepository;

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
    List<ReadDashboardServiceResponse.TopicSummary> topicSummaryList = calculateTopicSummaryList(user);
    List<DailySummary> dailySummaryList = calculateDailySummaryList(user);
    List<HourlySummary> hourlySummaryList = calculateHourlySummaryList(user);

    return ReadDashboardServiceResponse.builder()
        .quizTypeSummaryList(quizTypeSummaryList)
        .cumulativeSummary(cumulativeSummary)
        .topicSummaryList(topicSummaryList)
        .dailySummaryList(dailySummaryList)
        .hourlySummaryList(hourlySummaryList)
        .build();
  }

  private List<ReadDashboardServiceResponse.QuizTypeSummary> calculateQuizTypeSummaryList(User user) {
    LocalDate today = LocalDate.now();

    Map<QuizType, QuizTypeCounts> aggregateCountMap = new EnumMap<>(QuizType.class);

    aggregatePastSummaryList(user, today, aggregateCountMap);
    aggregateTodaySolveHistoryList(user, today, aggregateCountMap);

    return toQuizTypeSummaryList(aggregateCountMap);
  }

  private List<ReadDashboardServiceResponse.TopicSummary> calculateTopicSummaryList(User user) {

    LocalDate today = LocalDate.now();
    LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
    LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);

    List<SolveHistoryRepository.TopicSummary> summaries =
            solveHistoryRepository.findMonthlyTopicSummary(
                    user,
                    startOfMonth,
                    startOfNextMonth,
                    PageRequest.of(0, 6)
            );

    return summaries.stream()
            .map(s -> {
              int solved = Optional.ofNullable(s.getTotalCount())
                      .map(v -> v.intValue())
                      .orElse(0);

              int correct = Optional.ofNullable(s.getCorrectCount())
                      .map(v -> v.intValue())
                      .orElse(0);

              int wrong = solved - correct;


              return new ReadDashboardServiceResponse.TopicSummary(
                      s.getTopic(),
                      solved,
                      correct,
                      wrong
              );
            })
            .toList();
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

  private List<DailySummary> calculateDailySummaryList(User user) {
    LocalDate today = LocalDate.now();

    List<DailySummary> pastDailySummaryList = getPastDailySummary(user, today);
    List<DailySummary> todayDailySummaryList = getTodayDailySummary(user, today);

    List<DailySummary> dailySummaryList = new ArrayList<>(pastDailySummaryList);
    dailySummaryList.addAll(todayDailySummaryList);
    return dailySummaryList;
  }

  private List<DailySummary> getPastDailySummary(User user, LocalDate today) {
    LocalDate startOfMonth = today.withDayOfMonth(1);
    LocalDate yesterday = today.minusDays(1);

    if (startOfMonth.isAfter(yesterday)) {
      return Collections.emptyList();
    }

    return solveHourlySummaryRepository
        .findDailySummaryByUserAndDateBetween(user, startOfMonth, yesterday)
        .stream()
        .map(summary -> new DailySummary(
            summary.getDate(),
            Optional.ofNullable(summary.getSolvedCount()).map(Long::intValue).orElse(0)
        ))
        .toList();
  }

  private List<DailySummary> getTodayDailySummary(User user, LocalDate today) {
    return solveHistoryRepository
        .findDailySummaryByUserAndDate(user, today)
        .stream()
        .map(summary -> new DailySummary(
            summary.getDate(),
            Optional.ofNullable(summary.getSolvedCount()).map(Long::intValue).orElse(0)
        ))
        .toList();
  }

  private List<HourlySummary> calculateHourlySummaryList(User user) {
    LocalDate today = LocalDate.now();
    LocalDate startOfMonth = today.withDayOfMonth(1);
    LocalDate yesterday = today.minusDays(1);

    Map<Integer, Integer> hourlyCountMap = new HashMap<>();
    for (int slot : TIME_SLOTS) {
      hourlyCountMap.put(slot, 0);
    }

    if (!startOfMonth.isAfter(yesterday)) {
      aggregatePastHourlyData(user, startOfMonth, yesterday, hourlyCountMap);
    }

    aggregateTodayHourlyData(user, today, hourlyCountMap);

    return Arrays.stream(TIME_SLOTS)
        .mapToObj(slot -> new HourlySummary(slot, hourlyCountMap.get(slot)))
        .toList();
  }

  private void aggregatePastHourlyData(User user, LocalDate startDate, LocalDate endDate,
                                          Map<Integer, Integer> hourlyCountMap) {
    List<SolveHourlySummary> hourlySummaryList =
        solveHourlySummaryRepository.findByUserAndDateBetween(user, startDate, endDate);

    for (SolveHourlySummary summary : hourlySummaryList) {
      int hour = summary.getHour();
      int count = summary.getSolvedCount();

      int targetSlot = findTimeSlot(hour);
      hourlyCountMap.merge(targetSlot, count, Integer::sum);
    }
  }

  private void aggregateTodayHourlyData(User user, LocalDate today,
                                           Map<Integer, Integer> hourlyCountMap) {
    List<SolveHistoryRepository.HourlySummary> todayHourlyData =
        solveHistoryRepository.findHourlySummaryByUserAndDate(user, today);

    for (SolveHistoryRepository.HourlySummary summary : todayHourlyData) {
      Integer hour = summary.getHourOfDay();
      if (hour == null) {
        continue;
      }

      int count = Optional.ofNullable(summary.getSolvedCount()).map(Long::intValue).orElse(0);
      int targetSlot = findTimeSlot(hour);
      hourlyCountMap.merge(targetSlot, count, Integer::sum);
    }
  }

  private int findTimeSlot(int hour) {
    for (int i = TIME_SLOTS.length - 1; i >= 0; i--) {
      if (hour >= TIME_SLOTS[i]) {
        return TIME_SLOTS[i];
      }
    }
    return TIME_SLOTS[0];
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
    private List<TopicSummary> topicSummaryList;
    private List<DailySummary> dailySummaryList;
    private List<HourlySummary> hourlySummaryList;

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

    public record TopicSummary(
        String topic,
        int solvedCount,
        int correctCount,
        int wrongCount
    ){}

    public record DailySummary(
        LocalDate date,
        int solvedCount
    ){}

    public record HourlySummary(
        int startHour,
        int solvedCount
    ){}
  }
}