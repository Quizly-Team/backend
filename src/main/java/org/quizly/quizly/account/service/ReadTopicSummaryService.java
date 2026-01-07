package org.quizly.quizly.account.service;

import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.repository.SolveHistoryRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadTopicSummaryService implements BaseService<ReadTopicSummaryService.ReadTopicSummaryRequest, ReadTopicSummaryService.ReadTopicSummaryResponse> {

  private final SolveHistoryRepository solveHistoryRepository;

  @Override
  public ReadTopicSummaryResponse execute(ReadTopicSummaryRequest request) {
    if (request == null || !request.isValid()) {
      return ReadTopicSummaryResponse.builder()
          .success(false)
          .errorCode(ReadTopicSummaryErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    User user = request.getUser();
    List<ReadTopicSummaryResponse.TopicSummary> topicSummaryList = readTopicSummaryList(user);

    return ReadTopicSummaryResponse.builder()
        .topicSummaryList(topicSummaryList)
        .build();
  }

  private List<ReadTopicSummaryResponse.TopicSummary> readTopicSummaryList(User user) {
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

          return new ReadTopicSummaryResponse.TopicSummary(
              s.getTopic(),
              solved,
              correct,
              wrong
          );
        })
        .toList();
  }

  @Getter
  @RequiredArgsConstructor
  public enum ReadTopicSummaryErrorCode implements BaseErrorCode<DomainException> {
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
  public static class ReadTopicSummaryRequest implements BaseRequest {
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
  public static class ReadTopicSummaryResponse extends BaseResponse<ReadTopicSummaryErrorCode> {
    private List<TopicSummary> topicSummaryList;

    public record TopicSummary(
        String topic,
        int solvedCount,
        int correctCount,
        int wrongCount
    ){}
  }
}
