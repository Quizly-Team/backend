package org.quizly.quizly.account.service;

import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.repository.SolveHistoryRepository;
import org.quizly.quizly.core.domin.repository.SolveHourlySummaryRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadDailySummaryService implements BaseService<ReadDailySummaryService.ReadDailySummaryRequest, ReadDailySummaryService.ReadDailySummaryResponse> {

  private final SolveHistoryRepository solveHistoryRepository;
  private final SolveHourlySummaryRepository solveHourlySummaryRepository;

  @Override
  public ReadDailySummaryResponse execute(ReadDailySummaryRequest request) {
    if (request == null || !request.isValid()) {
      return ReadDailySummaryResponse.builder()
          .success(false)
          .errorCode(ReadDailySummaryErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    User user = request.getUser();
    List<ReadDailySummaryResponse.DailySummary> dailySummaryList = readDailySummaryList(user);

    return ReadDailySummaryResponse.builder()
        .dailySummaryList(dailySummaryList)
        .build();
  }

  private List<ReadDailySummaryResponse.DailySummary> readDailySummaryList(User user) {
    LocalDate today = LocalDate.now();

    List<ReadDailySummaryResponse.DailySummary> pastDailySummaryList = getPastDailySummary(user, today);
    List<ReadDailySummaryResponse.DailySummary> todayDailySummaryList = getTodayDailySummary(user, today);

    List<ReadDailySummaryResponse.DailySummary> dailySummaryList = new ArrayList<>(pastDailySummaryList);
    dailySummaryList.addAll(todayDailySummaryList);
    return dailySummaryList;
  }

  private List<ReadDailySummaryResponse.DailySummary> getPastDailySummary(User user, LocalDate today) {
    LocalDate startOfMonth = today.withDayOfMonth(1);
    LocalDate yesterday = today.minusDays(1);

    if (startOfMonth.isAfter(yesterday)) {
      return Collections.emptyList();
    }

    return solveHourlySummaryRepository
        .findDailySummaryByUserAndDateBetween(user, startOfMonth, yesterday)
        .stream()
        .map(summary -> new ReadDailySummaryResponse.DailySummary(
            summary.getDate(),
            Optional.ofNullable(summary.getSolvedCount()).map(Long::intValue).orElse(0)
        ))
        .toList();
  }

  private List<ReadDailySummaryResponse.DailySummary> getTodayDailySummary(User user, LocalDate today) {
    return solveHistoryRepository
        .findDailySummaryByUserAndDate(user, today)
        .stream()
        .map(summary -> new ReadDailySummaryResponse.DailySummary(
            summary.getDate(),
            Optional.ofNullable(summary.getSolvedCount()).map(Long::intValue).orElse(0)
        ))
        .toList();
  }

  @Getter
  @RequiredArgsConstructor
  public enum ReadDailySummaryErrorCode implements BaseErrorCode<DomainException> {
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
  public static class ReadDailySummaryRequest implements BaseRequest {
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
  public static class ReadDailySummaryResponse extends BaseResponse<ReadDailySummaryErrorCode> {
    private List<DailySummary> dailySummaryList;

    public record DailySummary(
        LocalDate date,
        int solvedCount
    ){}
  }
}
