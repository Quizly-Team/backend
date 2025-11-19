package org.quizly.quizly.quiz.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
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
import org.quizly.quizly.core.util.AsyncTaskUtil;
import org.quizly.quizly.core.util.TextProcessingUtil;
import org.quizly.quizly.external.clova.dto.Response.Hcx007QuizResponse;
import org.quizly.quizly.oauth.UserPrincipal;
import org.quizly.quizly.quiz.service.CreateMemberQuizzesService.CreateMemberQuizzesRequest;
import org.quizly.quizly.quiz.service.CreateMemberQuizzesService.CreateMemberQuizzesResponse;
import org.quizly.quizly.quiz.service.CreateQuizService.CreateQuizRequest;
import org.quizly.quizly.quiz.service.CreateQuizService.CreateQuizResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class CreateMemberQuizzesService implements BaseService<CreateMemberQuizzesRequest, CreateMemberQuizzesResponse> {

  private final CreateQuizService createQuizService;
  private final QuizRepository quizRepository;
  private final UserRepository userRepository;

  private static final int DEFAULT_QUIZ_COUNT = 10;
  private static final int DEFAULT_QUIZ_BATCH_SIZE = 3;
  private static final int DEFAULT_CHUNK_SIZE = 1000;
  private static final int DEFAULT_CHUNK_OVERLAP = 100;

  @Override
  public CreateMemberQuizzesResponse execute(CreateMemberQuizzesRequest request) {
    if (request == null || !request.isValid()) {
      return CreateMemberQuizzesResponse.builder()
          .success(false)
          .errorCode(CreateMemberQuizzesErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    String providerId = request.getUserPrincipal().getProviderId();
    if (providerId == null || providerId.isEmpty()) {
      return CreateMemberQuizzesResponse.builder()
          .success(false)
          .errorCode(CreateMemberQuizzesErrorCode.NOT_EXIST_PROVIDER_ID)
          .build();
    }
    User user = userRepository.findByProviderId(providerId);
    if (user == null) {
      log.error("[CreateMemberQuizzesService] User not found for providerId: {}", providerId);
      return CreateMemberQuizzesResponse.builder()
          .success(false)
          .errorCode(CreateMemberQuizzesErrorCode.NOT_FOUND_USER)
          .build();
    }

    List<String> chunkList = TextProcessingUtil.createChunkList(
        request.getPlainText(), DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
    if (chunkList.isEmpty()) {
      log.error("[CreateMemberQuizzesService] Failed to create chunks from plainText");
      return CreateMemberQuizzesResponse.builder()
          .success(false)
          .errorCode(CreateMemberQuizzesErrorCode.FAILED_CREATE_CHUNK)
          .build();
    }

    Quiz.QuizType type = request.getType();
    List<CompletableFuture<CreateQuizResponse>> futures = AsyncTaskUtil.requestAsyncTasks(
        chunkList,
        DEFAULT_QUIZ_COUNT,
        DEFAULT_QUIZ_BATCH_SIZE,
        (chunk, batchSize) -> createQuizService.execute(
            CreateQuizRequest.builder()
                .type(type)
                .quizCount(batchSize)
                .plainText(chunk)
                .build()
        ),
        "CreateMemberQuizzesService"
    );

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    List<Hcx007QuizResponse> hcx007QuizResponseList = AsyncTaskUtil.joinAsyncTasks(futures, response -> {
      if (response.isSuccess()) {
        return response.getHcx007QuizResponseList();
      }
      return null;
    });

    if (hcx007QuizResponseList.isEmpty() || hcx007QuizResponseList.size() < DEFAULT_QUIZ_COUNT) {
      log.info("[CreateMemberQuizzesService] No quizzes were generated from Clova Studio.");
      return CreateMemberQuizzesResponse.builder()
          .success(false)
          .errorCode(CreateMemberQuizzesErrorCode.CLOVA_QUIZ_GENERATION_FAILED)
          .build();
    }

    List<Quiz> quizList = saveQuiz(
        hcx007QuizResponseList.stream()
            .limit(DEFAULT_QUIZ_COUNT)
            .collect(Collectors.toList()),
        user);

    return CreateMemberQuizzesResponse.builder()
        .quizList(quizList)
        .build();
  }

  private List<Quiz> saveQuiz(List<Hcx007QuizResponse> hcx007QuizResponseList, User user) {
    List<Quiz> quizList = hcx007QuizResponseList.stream()
        .map(hcx007QuizResponse -> Quiz.builder()
            .quizText(hcx007QuizResponse.getQuiz())
            .answer(hcx007QuizResponse.getAnswer())
            .quizType(hcx007QuizResponse.getType())
            .explanation(hcx007QuizResponse.getExplanation())
            .options(hcx007QuizResponse.getOptions())
            .topic("테스트")
            .user(user)
            .guest(false)
            .build())
        .collect(Collectors.toList());
    return quizRepository.saveAll(quizList);
  }

  @Getter
  @RequiredArgsConstructor
  public enum CreateMemberQuizzesErrorCode implements BaseErrorCode<DomainException> {

    NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
    NOT_EXIST_PROVIDER_ID(HttpStatus.BAD_REQUEST, "Provider ID가 존재하지 않습니다."),
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    FAILED_CREATE_CHUNK(HttpStatus.INTERNAL_SERVER_ERROR, "텍스트 청크 생성에 실패하였습니다."),
    FAILED_CREATE_CLOVA_REQUEST(HttpStatus.INTERNAL_SERVER_ERROR, "CLOVA 서버 요청 생성에 실패하였습니다."),
    CLOVA_QUIZ_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CLOVA 서버에서 퀴즈 생성에 실패하였습니다.");

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
  public static class CreateMemberQuizzesRequest implements BaseRequest {

    private String plainText;

    private Quiz.QuizType type;

    private UserPrincipal userPrincipal;

    @Override
    public boolean isValid() {
      return plainText != null && !plainText.isEmpty() && type != null && userPrincipal != null;
    }
  }

  @Getter
  @Setter
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class CreateMemberQuizzesResponse extends BaseResponse<CreateMemberQuizzesErrorCode> {
    private List<Quiz> quizList;
  }
}