package org.quizly.quizly.admin.service;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.admin.service.BatchAggregateSummaryService.BatchAggregateSummaryRequest;
import org.quizly.quizly.admin.service.BatchAggregateSummaryService.BatchAggregateSummaryResponse;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.repository.UserRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Log4j2
@Service
@RequiredArgsConstructor
public class BatchAggregateSummaryService implements BaseService<BatchAggregateSummaryRequest, BatchAggregateSummaryResponse> {

  private final JobLauncher jobLauncher;
  private final Job aggregateDailySummaryJob;
  private final UserRepository userRepository;

  @Override
  public BatchAggregateSummaryResponse execute(BatchAggregateSummaryRequest request) {
    if (request == null || !request.isValid()) {
      return BatchAggregateSummaryResponse.builder()
          .success(false)
          .errorCode(BatchAggregateSummaryErrorCode.INVALID_PARAMETER)
          .build();
    }

    String providerId = request.getUserPrincipal().getProviderId();
    if (providerId == null || providerId.isBlank()) {
      return BatchAggregateSummaryResponse.builder()
          .success(false)
          .errorCode(BatchAggregateSummaryErrorCode.INVALID_PARAMETER)
          .build();
    }

    var userOptional = userRepository.findByProviderId(providerId);
    if (userOptional.isEmpty()) {
      log.error("[BatchAggregateSummaryService] User not found for providerId: {}", providerId);
      return BatchAggregateSummaryResponse.builder()
          .success(false)
          .errorCode(BatchAggregateSummaryErrorCode.NOT_FOUND_USER)
          .build();
    }
    User user = userOptional.get();

    LocalDate targetDate = request.getTargetDate();
    LocalDate yesterday = LocalDate.now().minusDays(1);
    if (targetDate.isAfter(yesterday)) {
      return BatchAggregateSummaryResponse.builder()
          .success(false)
          .errorCode(BatchAggregateSummaryErrorCode.FUTURE_DATE_NOT_ALLOWED)
          .build();
    }

    try {
      JobParameters jobParameters = new JobParametersBuilder()
          .addString("targetDate", targetDate.toString())
          .addString("adminEmail", user.getEmail())
          .addLong("timestamp", System.currentTimeMillis())
          .toJobParameters();

      jobLauncher.run(aggregateDailySummaryJob, jobParameters);

      return BatchAggregateSummaryResponse.builder().build();

    } catch (Exception e) {
      log.error("[BatchAggregateSummaryService] Failed for date: {}", targetDate, e);
      return BatchAggregateSummaryResponse.builder()
          .success(false)
          .errorCode(BatchAggregateSummaryErrorCode.BATCH_EXECUTION_FAILED)
          .build();
    }
  }

  @Getter
  @RequiredArgsConstructor
  public enum BatchAggregateSummaryErrorCode implements BaseErrorCode<DomainException> {

    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 올바르지 않습니다."),
    FUTURE_DATE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "미래 날짜는 처리할 수 없습니다."),
    BATCH_EXECUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "배치 실행이 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public DomainException toException() {
      return new DomainException(httpStatus, this);
    }
  }

  @Getter
  @Builder
  public static class BatchAggregateSummaryRequest implements BaseRequest {
    private UserPrincipal userPrincipal;
    private LocalDate targetDate;

    @Override
    public boolean isValid() {
      return userPrincipal != null && targetDate != null;
    }
  }

  @Getter
  @SuperBuilder
  public static class BatchAggregateSummaryResponse extends BaseResponse<BatchAggregateSummaryErrorCode> {
  }
}