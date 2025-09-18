package org.quizly.quizly.quiz.service;

import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domin.entity.Quiz;
import org.quizly.quizly.core.domin.entity.SolveHistory;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.repository.QuizRepository;
import org.quizly.quizly.core.domin.repository.SolveHistoryRepository;
import org.quizly.quizly.core.domin.repository.UserRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.quizly.quizly.quiz.service.ReadQuizzesService.ReadQuizzesRequest;
import org.quizly.quizly.quiz.service.ReadQuizzesService.ReadQuizzesResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class ReadQuizzesService implements BaseService<ReadQuizzesRequest, ReadQuizzesResponse> {

  private final UserRepository userRepository;
  private final QuizRepository quizRepository;
  private final SolveHistoryRepository solveHistoryRepository;

  @Override
  public ReadQuizzesResponse execute(ReadQuizzesRequest request) {
    if (request == null || !request.isValid()) {
      return ReadQuizzesResponse.builder()
          .success(false)
          .errorCode(ReadQuizzesErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    String providerId = request.getUserPrincipal().getProviderId();
    if (providerId == null || providerId.isEmpty()) {
      return ReadQuizzesResponse.builder()
          .success(false)
          .errorCode(ReadQuizzesErrorCode.NOT_EXIST_PROVIDER_ID)
          .build();
    }
    User user = userRepository.findByProviderId(providerId);
    if (user == null) {
      log.error("[ReadQuizzesService] User not found for providerId: {}", providerId);
      return ReadQuizzesResponse.builder()
          .success(false)
          .errorCode(ReadQuizzesErrorCode.NOT_FOUND_USER)
          .build();
    }

    List<Quiz> quizList = quizRepository.findAllByUser(user);
    if (quizList.isEmpty()) {
      return ReadQuizzesResponse.builder()
          .success(true)
          .quizList(quizList)
          .solveHistoryList(Collections.emptyList())
          .build();
    }
    List<SolveHistory> latestSolveHistoryList = solveHistoryRepository.findLatestSolveHistoriesByUser(
        user);

    return ReadQuizzesResponse.builder()
        .quizList(quizList)
        .solveHistoryList(latestSolveHistoryList)
        .build();
  }

  @Getter
  @RequiredArgsConstructor
  public enum ReadQuizzesErrorCode implements BaseErrorCode<DomainException> {

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
  public static class ReadQuizzesRequest implements BaseRequest {

    private String groupType;
    private UserPrincipal userPrincipal;

    @Override
    public boolean isValid() {
      return groupType != null && userPrincipal != null;
    }
  }

  @Getter
  @Setter
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class ReadQuizzesResponse extends BaseResponse<ReadQuizzesErrorCode> {

    private List<Quiz> quizList;

    private List<SolveHistory> solveHistoryList;
  }
}
