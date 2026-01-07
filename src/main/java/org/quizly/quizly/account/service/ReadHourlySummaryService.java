package org.quizly.quizly.account.service;

import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domin.entity.SolveHourlySummary;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.repository.SolveHistoryRepository;
import org.quizly.quizly.core.domin.repository.SolveHourlySummaryRepository;
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
public class ReadHourlySummaryService implements BaseService<ReadHourlySummaryService.ReadHourlySummaryRequest, ReadHourlySummaryService.ReadHourlySummaryResponse> {

  private static final int[] TIME_SLOTS = {0, 6, 9, 12, 15, 18, 21};

  private final SolveHistoryRepository solveHistoryRepository;
  private final SolveHourlySummaryRepository solveHourlySummaryRepository;

  @Override
  public ReadHourlySummaryResponse execute(ReadHourlySummaryRequest request) {
    if (request == null || !request.isValid()) {
      return ReadHourlySummaryResponse.builder()
          .success(false)
          .errorCode(ReadHourlySummaryErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    User user = request.getUser();
    List<ReadHourlySummaryResponse.HourlySummary> hourlySummaryList = readHourlySummaryList(user);

    return ReadHourlySummaryResponse.builder()
        .hourlySummaryList(hourlySummaryList)
        .build();
  }

  private List<ReadHourlySummaryResponse.HourlySummary> readHourlySummaryList(User user) {
    LocalDate today = LocalDate.now();
    LocalDate startOfMonth = today.withDayOfMonth(1);
    LocalDate yesterday = today.minusDays(1);

    Map<Integer, Integer> pastHourlyMap = new HashMap<>();
    if (!startOfMonth.isAfter(yesterday)) {
      pastHourlyMap = getPastHourlyDataMap(user, startOfMonth, yesterday);
    }

    Map<Integer, Integer> todayHourlyMap = getTodayHourlyDataMap(user, today);

    Map<Integer, Integer> hourlyCountMap = new HashMap<>();
    for (int slot : TIME_SLOTS) {
      hourlyCountMap.put(slot, 0);
    }

    pastHourlyMap.forEach((slot, count) -> hourlyCountMap.merge(slot, count, Integer::sum));
    todayHourlyMap.forEach((slot, count) -> hourlyCountMap.merge(slot, count, Integer::sum));

    return Arrays.stream(TIME_SLOTS)
        .mapToObj(slot -> new ReadHourlySummaryResponse.HourlySummary(slot, hourlyCountMap.get(slot)))
        .toList();
  }

  private Map<Integer, Integer> getPastHourlyDataMap(User user, LocalDate startDate, LocalDate endDate) {
    Map<Integer, Integer> map = new HashMap<>();
    List<SolveHourlySummary> hourlySummaryList =
        solveHourlySummaryRepository.findByUserAndDateBetween(user, startDate, endDate);

    for (SolveHourlySummary summary : hourlySummaryList) {
      int hour = summary.getHour();
      int count = summary.getSolvedCount();

      int targetSlot = findTimeSlot(hour);
      map.merge(targetSlot, count, Integer::sum);
    }

    return map;
  }

  private Map<Integer, Integer> getTodayHourlyDataMap(User user, LocalDate today) {
    Map<Integer, Integer> map = new HashMap<>();
    List<SolveHistoryRepository.HourlySummary> todayHourlyData =
        solveHistoryRepository.findHourlySummaryByUserAndDate(user, today);

    for (SolveHistoryRepository.HourlySummary summary : todayHourlyData) {
      Integer hour = summary.getHourOfDay();
      if (hour == null) {
        continue;
      }

      int count = Optional.ofNullable(summary.getSolvedCount()).map(Long::intValue).orElse(0);
      int targetSlot = findTimeSlot(hour);
      map.merge(targetSlot, count, Integer::sum);
    }

    return map;
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
  @RequiredArgsConstructor
  public enum ReadHourlySummaryErrorCode implements BaseErrorCode<DomainException> {
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
  public static class ReadHourlySummaryRequest implements BaseRequest {
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
  public static class ReadHourlySummaryResponse extends BaseResponse<ReadHourlySummaryErrorCode> {
    private List<HourlySummary> hourlySummaryList;

    public record HourlySummary(
        int startHour,
        int solvedCount
    ){}
  }
}
