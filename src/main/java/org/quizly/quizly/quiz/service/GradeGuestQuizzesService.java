package org.quizly.quizly.quiz.service;

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
import org.quizly.quizly.core.domin.repository.QuizRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.quiz.service.GradeGuestQuizzesService.GradeGuestQuizzesRequest;
import org.quizly.quizly.quiz.service.GradeGuestQuizzesService.GradeGuestQuizzesResponse;
import org.quizly.quizly.quiz.service.GradeMemberQuizzesService.GradeMemberQuizzesErrorCode;
import org.quizly.quizly.quiz.service.GradeMemberQuizzesService.GradeMemberQuizzesResponse;
import org.quizly.quizly.quiz.service.GraderQuizService.GraderQuizRequest;
import org.quizly.quizly.quiz.service.GraderQuizService.GraderQuizResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class GradeGuestQuizzesService implements BaseService<GradeGuestQuizzesRequest, GradeGuestQuizzesResponse> {

  private final QuizRepository quizRepository;
  private final GraderQuizService graderQuizService;

  @Override
  public GradeGuestQuizzesResponse execute(GradeGuestQuizzesRequest request) {
    if (request == null || !request.isValid()) {
      return GradeGuestQuizzesResponse.builder()
          .success(false)
          .errorCode(GradeGuestQuizzesErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    Optional<Quiz> optionalQuiz = quizRepository.findById(request.getQuizId());
    if (optionalQuiz.isEmpty()) {
      return GradeGuestQuizzesResponse.builder()
          .success(false)
          .errorCode(GradeGuestQuizzesErrorCode.QUIZ_NOT_FOUND)
          .build();
    }

    Quiz quiz = optionalQuiz.get();

    GraderQuizResponse graderQuizResponse = graderQuizService.execute(
        GraderQuizRequest.builder()
            .answer(quiz.getAnswer())
            .userAnswer(request.getUserAnswer())
            .build());
    if (graderQuizResponse == null || !graderQuizResponse.isSuccess()) {
      return GradeGuestQuizzesResponse.builder()
          .success(false)
          .errorCode(GradeGuestQuizzesErrorCode.GRADE_FAILED)
          .build();
    }
    boolean isCorrect = graderQuizResponse.isCorrect();

    return GradeGuestQuizzesResponse.builder()
        .quiz(quiz)
        .isCorrect(isCorrect)
        .build();
  }


  @Getter
  @RequiredArgsConstructor
  public enum GradeGuestQuizzesErrorCode implements BaseErrorCode<DomainException> {

    NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
    QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "퀴즈를 찾을 수 없습니다."),
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
  public static class GradeGuestQuizzesRequest implements BaseRequest {

    private Long quizId;

    private String userAnswer;

    @Override
    public boolean isValid() {
      return quizId != null && userAnswer != null;
    }
  }

  @Getter
  @Setter
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class GradeGuestQuizzesResponse extends BaseResponse<GradeGuestQuizzesErrorCode> {
    private Quiz quiz;
    private boolean isCorrect;
  }

}