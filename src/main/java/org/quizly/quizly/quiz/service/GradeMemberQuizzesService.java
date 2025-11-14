package org.quizly.quizly.quiz.service;

import java.time.LocalDateTime;
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
import org.quizly.quizly.quiz.service.GradeMemberQuizzesService.GradeMemberQuizzesRequest;
import org.quizly.quizly.quiz.service.GradeMemberQuizzesService.GradeMemberQuizzesResponse;
import org.quizly.quizly.quiz.service.GraderQuizService.GraderQuizRequest;
import org.quizly.quizly.quiz.service.GraderQuizService.GraderQuizResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class GradeMemberQuizzesService implements
    BaseService<GradeMemberQuizzesRequest, GradeMemberQuizzesResponse> {

  private final QuizRepository quizRepository;
  private final UserRepository userRepository;
  private final SolveHistoryRepository solveHistoryRepository;
  private final GraderQuizService graderQuizService;

  @Override
  public GradeMemberQuizzesResponse execute(GradeMemberQuizzesRequest request) {
    if (request == null || !request.isValid()) {
      return GradeMemberQuizzesResponse.builder()
          .success(false)
          .errorCode(GradeMemberQuizzesErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    String providerId = request.getUserPrincipal().getProviderId();
    if (providerId == null || providerId.isBlank()) {
      return GradeMemberQuizzesResponse.builder()
          .success(false)
          .errorCode(GradeMemberQuizzesErrorCode.NOT_EXIST_PROVIDER_ID)
          .build();
    }
    User user = userRepository.findByProviderId(providerId);
    if (user == null) {
      log.error("[GradeMemberQuizzesService] User not found for providerId: {}", providerId);
      return GradeMemberQuizzesResponse.builder()
          .success(false)
          .errorCode(GradeMemberQuizzesErrorCode.NOT_FOUND_USER)
          .build();
    }

    Optional<Quiz> optionalQuiz = quizRepository.findById(request.getQuizId());
    if (optionalQuiz.isEmpty()) {
      return GradeMemberQuizzesResponse.builder()
          .success(false)
          .errorCode(GradeMemberQuizzesErrorCode.QUIZ_NOT_FOUND)
          .build();
    }
    Quiz quiz = optionalQuiz.get();

    if (quiz.getUser() == null || !quiz.getUser().equals(user)) {
      log.error("[GradeMemberQuizzesService] Cannot solve other quiz userId: {}, quizId: {} ", user.getId(), quiz.getId());
      return GradeMemberQuizzesResponse.builder()
          .success(false)
          .errorCode(GradeMemberQuizzesErrorCode.CANNOT_SOLVE_OTHER_QUIZ)
          .build();
    }

    GraderQuizResponse graderQuizResponse = graderQuizService.execute(
        GraderQuizRequest.builder()
            .answer(quiz.getAnswer())
            .userAnswer(request.getUserAnswer())
            .build());
    if (graderQuizResponse == null || !graderQuizResponse.isSuccess()) {
      return GradeMemberQuizzesResponse.builder()
          .success(false)
          .errorCode(GradeMemberQuizzesErrorCode.GRADE_FAILED)
          .build();
    }
    boolean isCorrect = graderQuizResponse.isCorrect();

    saveSolveHistory(user, quiz, request.getUserAnswer(), isCorrect, request.getSolveTime());

    return GradeMemberQuizzesResponse.builder()
        .quiz(quiz)
        .isCorrect(isCorrect)
        .build();
  }

  private void saveSolveHistory(
          User user,
          Quiz quiz,
          String userAnswer,
          boolean isCorrect,
          Double solveTime
  ) {
    SolveHistory solveHistory = SolveHistory.builder()
            .user(user)
            .quiz(quiz)
            .isCorrect(isCorrect)
            .userAnswer(userAnswer)
            .solveTime(solveTime)
            .submittedAt(LocalDateTime.now())
            .build();

    solveHistoryRepository.save(solveHistory);
  }


  @Getter
  @RequiredArgsConstructor
  public enum GradeMemberQuizzesErrorCode implements BaseErrorCode<DomainException> {

    NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
    QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "퀴즈를 찾을 수 없습니다."),
    NOT_EXIST_PROVIDER_ID(HttpStatus.BAD_REQUEST, "Provider ID가 존재하지 않습니다."),
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    CANNOT_SOLVE_OTHER_QUIZ(HttpStatus.FORBIDDEN, "다른 유저가 만든 퀴즈는 풀 수 없습니다."),
    GRADE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "채점에 실패하였습니다.");

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
  public static class GradeMemberQuizzesRequest implements BaseRequest {

    private Long quizId;

    private String userAnswer;

    private UserPrincipal userPrincipal;

    private Double solveTime;

    @Override
    public boolean isValid() {
      return quizId != null && userAnswer != null && userPrincipal != null && solveTime != null && solveTime >= 0;
    }
  }

  @Getter
  @Setter
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class GradeMemberQuizzesResponse extends BaseResponse<GradeMemberQuizzesErrorCode> {

    private Quiz quiz;
    private boolean isCorrect;
  }

}
