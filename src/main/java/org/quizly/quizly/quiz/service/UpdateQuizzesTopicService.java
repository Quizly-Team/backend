package org.quizly.quizly.quiz.service;

import java.util.List;
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
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.repository.QuizRepository;
import org.quizly.quizly.core.domin.repository.UserRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.quizly.quizly.quiz.service.UpdateQuizzesTopicService.UpdateQuizzesTopicRequest;
import org.quizly.quizly.quiz.service.UpdateQuizzesTopicService.UpdateQuizzesTopicResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateQuizzesTopicService implements
    BaseService<UpdateQuizzesTopicRequest, UpdateQuizzesTopicResponse> {

  private final QuizRepository quizRepository;

  private final UserRepository userRepository;

  @Override
  public UpdateQuizzesTopicResponse execute(UpdateQuizzesTopicRequest request) {
    if (request == null || !request.isValid()) {
      return UpdateQuizzesTopicResponse.builder()
          .success(false)
          .errorCode(UpdateQuizzesTopicErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    String providerId = request.getUserPrincipal().getProviderId();
    if (providerId == null || providerId.isEmpty()) {
      return UpdateQuizzesTopicResponse.builder()
          .success(false)
          .errorCode(UpdateQuizzesTopicErrorCode.NOT_EXIST_PROVIDER_ID)
          .build();
    }
    Optional<User> userOptional = userRepository.findByProviderId(providerId);
    if (userOptional.isEmpty()) {
      log.error("[UpdateQuizzesTopicService] User not found for providerId: {}", providerId);
      return UpdateQuizzesTopicResponse.builder()
          .success(false)
          .errorCode(UpdateQuizzesTopicErrorCode.NOT_FOUND_USER)
          .build();
    }
    User user = userOptional.get();

    List<Quiz> quizList = quizRepository.findAllById(request.getQuizIdList());

    if (quizList.size() != request.getQuizIdList().size()) {
      return UpdateQuizzesTopicResponse.builder()
          .success(false)
          .errorCode(UpdateQuizzesTopicErrorCode.NOT_FOUND_QUIZ)
          .build();
    }

    if (!checkUserOwnsQuizList(quizList, user)) {
      return UpdateQuizzesTopicResponse.builder()
          .success(false)
          .errorCode(UpdateQuizzesTopicErrorCode.NOT_QUIZ_OWNER)
          .build();
    }
    quizList.forEach(quiz -> quiz.setTopic(request.getTopic()));

    return UpdateQuizzesTopicResponse.builder().build();
  }

  private boolean checkUserOwnsQuizList(List<Quiz> quizList, User user) {
    return quizList.stream()
        .allMatch(quiz ->
            quiz.getUser() != null && quiz.getUser().getId().equals(user.getId())
        );
  }

  @Getter
  @RequiredArgsConstructor
  public enum UpdateQuizzesTopicErrorCode implements BaseErrorCode<DomainException> {

    NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
    NOT_EXIST_PROVIDER_ID(HttpStatus.BAD_REQUEST, "Provider ID가 존재하지 않습니다."),
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    NOT_FOUND_QUIZ(HttpStatus.NOT_FOUND, "문제를 찾을 수 없습니다."),
    NOT_QUIZ_OWNER(HttpStatus.FORBIDDEN, "문제를 수정할 권한이 없습니다.");

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
  public static class UpdateQuizzesTopicRequest implements BaseRequest {

    private String topic;

    private List<Long> quizIdList;

    private UserPrincipal userPrincipal;

    @Override
    public boolean isValid() {
      return quizIdList != null && !quizIdList.isEmpty() && userPrincipal != null;
    }
  }

  @Getter
  @Setter
  @SuperBuilder
  @NoArgsConstructor
  @ToString
  public static class UpdateQuizzesTopicResponse extends BaseResponse<UpdateQuizzesTopicErrorCode> {

  }

}
