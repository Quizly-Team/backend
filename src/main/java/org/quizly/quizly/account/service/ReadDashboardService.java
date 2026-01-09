package org.quizly.quizly.account.service;

import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.account.service.ReadCumulativeSummaryService.ReadCumulativeSummaryRequest;
import org.quizly.quizly.account.service.ReadCumulativeSummaryService.ReadCumulativeSummaryResponse;
import org.quizly.quizly.account.service.ReadDailySummaryService.ReadDailySummaryRequest;
import org.quizly.quizly.account.service.ReadDailySummaryService.ReadDailySummaryResponse;
import org.quizly.quizly.account.service.ReadHourlySummaryService.ReadHourlySummaryRequest;
import org.quizly.quizly.account.service.ReadHourlySummaryService.ReadHourlySummaryResponse;
import org.quizly.quizly.account.service.ReadQuizTypeSummaryService.ReadQuizTypeSummaryRequest;
import org.quizly.quizly.account.service.ReadQuizTypeSummaryService.ReadQuizTypeSummaryResponse;
import org.quizly.quizly.account.service.ReadTodaySummaryService.ReadTodaySummaryRequest;
import org.quizly.quizly.account.service.ReadTodaySummaryService.ReadTodaySummaryResponse;
import org.quizly.quizly.account.service.ReadTopicSummaryService.ReadTopicSummaryRequest;
import org.quizly.quizly.account.service.ReadTopicSummaryService.ReadTopicSummaryResponse;
import org.quizly.quizly.account.service.ReadUserService.ReadUserRequest;
import org.quizly.quizly.account.service.ReadUserService.ReadUserResponse;
import org.quizly.quizly.account.support.DashboardAiAnalysisInputBuilder;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadDashboardService implements BaseService<ReadDashboardService.ReadDashboardRequest, ReadDashboardService.ReadDashboardServiceResponse> {

  private final ReadUserService readUserService;
  private final ReadQuizTypeSummaryService readQuizTypeSummaryService;
  private final ReadTopicSummaryService readTopicSummaryService;
  private final ReadCumulativeSummaryService readCumulativeSummaryService;
  private final ReadDailySummaryService readDailySummaryService;
  private final ReadHourlySummaryService readHourlySummaryService;
  private final ReadTodaySummaryService readTodaySummaryService;
  private final ReadAiAnalysisService readAiAnalysisService;
  private final DashboardAiAnalysisInputBuilder dashboardAiAnalysisInputBuilder;


  @Override
  public ReadDashboardServiceResponse execute(ReadDashboardRequest request) {
    if (request == null || !request.isValid()) {
      return ReadDashboardServiceResponse.builder()
          .success(false)
          .errorCode(ReadDashboardErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    ReadUserResponse readUserResponse = readUserService.execute(
        ReadUserRequest.builder()
            .userPrincipal(request.getUserPrincipal())
            .build()
    );


    if (!readUserResponse.isSuccess()) {
      return ReadDashboardServiceResponse.builder()
          .success(false)
          .errorCode(ReadDashboardErrorCode.NOT_FOUND_USER)
          .build();
    }
    User user = readUserResponse.getUser();

    ReadTodaySummaryResponse readTodaySummaryResponse = readTodaySummaryService.execute(
        ReadTodaySummaryRequest.builder()
            .user(user)
            .build()
    );

    if (!readTodaySummaryResponse.isSuccess()) {
      return ReadDashboardServiceResponse.builder()
          .success(false)
          .errorCode(ReadDashboardErrorCode.FAILED_TO_GET_TODAY_SUMMARY)
          .build();
    }

    ReadQuizTypeSummaryResponse quizTypeSummaryResponse = readQuizTypeSummaryService.execute(
        ReadQuizTypeSummaryRequest.builder()
            .user(user)
            .build()
    );

    if (!quizTypeSummaryResponse.isSuccess()) {
      return ReadDashboardServiceResponse.builder()
          .success(false)
          .errorCode(ReadDashboardErrorCode.FAILED_TO_GET_QUIZ_TYPE_SUMMARY)
          .build();
    }

    ReadTopicSummaryResponse topicSummaryResponse = readTopicSummaryService.execute(
        ReadTopicSummaryRequest.builder()
            .user(user)
            .build()
    );

    if (!topicSummaryResponse.isSuccess()) {
      return ReadDashboardServiceResponse.builder()
          .success(false)
          .errorCode(ReadDashboardErrorCode.FAILED_TO_GET_TOPIC_SUMMARY)
          .build();
    }

    ReadCumulativeSummaryResponse cumulativeSummaryResponse = readCumulativeSummaryService.execute(
        ReadCumulativeSummaryRequest.builder()
            .quizTypeSummaryList(quizTypeSummaryResponse.getQuizTypeSummaryList())
            .build()
    );

    if (!cumulativeSummaryResponse.isSuccess()) {
      return ReadDashboardServiceResponse.builder()
          .success(false)
          .errorCode(ReadDashboardErrorCode.FAILED_TO_GET_CUMULATIVE_SUMMARY)
          .build();
    }

    ReadDailySummaryResponse dailySummaryResponse = readDailySummaryService.execute(
        ReadDailySummaryRequest.builder()
            .user(user)
            .build()
    );

    if (!dailySummaryResponse.isSuccess()) {
      return ReadDashboardServiceResponse.builder()
          .success(false)
          .errorCode(ReadDashboardErrorCode.FAILED_TO_GET_DAILY_SUMMARY)
          .build();
    }

    ReadHourlySummaryResponse hourlySummaryResponse = readHourlySummaryService.execute(
        ReadHourlySummaryRequest.builder()
            .user(user)
            .build()
    );

    if (!hourlySummaryResponse.isSuccess()) {
      return ReadDashboardServiceResponse.builder()
          .success(false)
          .errorCode(ReadDashboardErrorCode.FAILED_TO_GET_HOURLY_SUMMARY)
          .build();
    }
    String aiInput = dashboardAiAnalysisInputBuilder.build(
            readTodaySummaryResponse.getTodaySummary(),
            quizTypeSummaryResponse.getQuizTypeSummaryList(),
            topicSummaryResponse.getTopicSummaryList()
    );

    ReadAiAnalysisService.ReadAiAnalysisResponse aiResponse =
            readAiAnalysisService.execute(
                    ReadAiAnalysisService.ReadAiAnalysisRequest.builder()
                            .analysisTargetText(aiInput)
                            .promptPath("prompt/account/dashboard_analysis.txt")
                            .build()
            );

    String aiResult = aiResponse.isSuccess()
            ? aiResponse.getAnalysisResult()
            : null;

    return ReadDashboardServiceResponse.builder()
        .todaySummary(readTodaySummaryResponse.getTodaySummary())
        .quizTypeSummaryList(quizTypeSummaryResponse.getQuizTypeSummaryList())
        .topicSummaryList(topicSummaryResponse.getTopicSummaryList())
        .cumulativeSummary(cumulativeSummaryResponse.getCumulativeSummary())
        .dailySummaryList(dailySummaryResponse.getDailySummaryList())
        .hourlySummaryList(hourlySummaryResponse.getHourlySummaryList())
        .aiAnalysisResult(aiResult)
        .build();
  }



  @Getter
  @RequiredArgsConstructor
  public enum ReadDashboardErrorCode implements BaseErrorCode<DomainException> {
    NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
    NOT_EXIST_PROVIDER_ID(HttpStatus.BAD_REQUEST, "Provider ID가 존재하지 않습니다."),
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    FAILED_TO_GET_TODAY_SUMMARY(HttpStatus.INTERNAL_SERVER_ERROR, "오늘 학습 요약 조회에 실패했습니다."),
    FAILED_TO_GET_QUIZ_TYPE_SUMMARY(HttpStatus.INTERNAL_SERVER_ERROR, "퀴즈 타입 통계 조회에 실패했습니다."),
    FAILED_TO_GET_TOPIC_SUMMARY(HttpStatus.INTERNAL_SERVER_ERROR, "주제별 통계 조회에 실패했습니다."),
    FAILED_TO_GET_CUMULATIVE_SUMMARY(HttpStatus.INTERNAL_SERVER_ERROR, "누적 통계 조회에 실패했습니다."),
    FAILED_TO_GET_DAILY_SUMMARY(HttpStatus.INTERNAL_SERVER_ERROR, "일별 통계 조회에 실패했습니다."),
    FAILED_TO_GET_HOURLY_SUMMARY(HttpStatus.INTERNAL_SERVER_ERROR, "시간대별 통계 조회에 실패했습니다.");

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
    private ReadTodaySummaryResponse.TodaySummary todaySummary;
    private ReadCumulativeSummaryResponse.CumulativeSummary cumulativeSummary;
    private List<ReadQuizTypeSummaryResponse.QuizTypeSummary> quizTypeSummaryList;
    private List<ReadTopicSummaryResponse.TopicSummary> topicSummaryList;
    private List<ReadDailySummaryResponse.DailySummary> dailySummaryList;
    private List<ReadHourlySummaryResponse.HourlySummary> hourlySummaryList;
    private String aiAnalysisResult;
  }
}
