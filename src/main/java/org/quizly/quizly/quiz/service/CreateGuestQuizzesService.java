package org.quizly.quizly.quiz.service;

import java.util.concurrent.CompletableFuture;
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
import org.quizly.quizly.core.util.AsyncTaskUtil;
import org.quizly.quizly.core.util.TextProcessingUtil;
import org.quizly.quizly.quiz.dto.response.GeneratedQuizResponse;
import org.quizly.quizly.quiz.service.CreateGuestQuizzesService.CreateGuestQuizzesRequest;
import org.quizly.quizly.quiz.service.CreateGuestQuizzesService.CreateGuestQuizzesResponse;
import org.quizly.quizly.quiz.service.CreateQuizService.CreateQuizRequest;
import org.quizly.quizly.quiz.service.CreateQuizService.CreateQuizResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class CreateGuestQuizzesService implements BaseService<CreateGuestQuizzesRequest, CreateGuestQuizzesResponse> {

  private final CreateQuizService createQuizService;
  private final QuizRepository quizRepository;

  private static final int DEFAULT_QUIZ_COUNT = 3;
  private static final int DEFAULT_QUIZ_BATCH_SIZE = 2;
  private static final int DEFAULT_CHUNK_SIZE = 500;
  private static final int DEFAULT_CHUNK_OVERLAP = 100;
  private static final String GUEST_TOPIC = "guest";

  @Override
  public CreateGuestQuizzesResponse execute(CreateGuestQuizzesRequest request) {
    if (request == null || !request.isValid()) {
      return CreateGuestQuizzesResponse.builder()
          .success(false)
          .errorCode(CreateGuestQuizzesErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    List<String> chunkList = TextProcessingUtil.createChunkList(
        request.getPlainText(), DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
    if (chunkList.isEmpty()) {
      log.error("[CreateGuestQuizzesService] Failed to create chunks from plainText");
      return CreateGuestQuizzesResponse.builder()
          .success(false)
          .errorCode(CreateGuestQuizzesErrorCode.FAILED_CREATE_CHUNK)
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
        "CreateGuestQuizzesService"
    );

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    List<GeneratedQuizResponse> generatedQuizResponseList = AsyncTaskUtil.joinAsyncTasks(futures, response -> {
      if (response.isSuccess()) {
        return response.getGeneratedQuizResponseList();
      }
      return null;
    });

    if (generatedQuizResponseList.isEmpty() || generatedQuizResponseList.size() < DEFAULT_QUIZ_COUNT) {
      log.info("[CreateGuestQuizzesService] No quizzes were generated from OpenAi.");
      return CreateGuestQuizzesResponse.builder()
          .success(false)
          .errorCode(CreateGuestQuizzesErrorCode.OPENAI_QUIZ_GENERATION_FAILED)
          .build();
    }

    List<Quiz> quizList = saveQuiz(
        generatedQuizResponseList.stream()
            .limit(DEFAULT_QUIZ_COUNT)
            .collect(Collectors.toList()));

    return CreateGuestQuizzesResponse.builder().quizList(quizList).build();
  }

  private List<Quiz> saveQuiz(List<GeneratedQuizResponse> generatedQuizResponseList) {
    List<Quiz> quizList = generatedQuizResponseList.stream()
        .map(generatedQuizResponse -> Quiz.builder()
            .quizText(generatedQuizResponse.getQuiz())
            .answer(generatedQuizResponse.getAnswer())
            .quizType(generatedQuizResponse.getType())
            .explanation(generatedQuizResponse.getExplanation())
            .options(generatedQuizResponse.getOptions())
            .topic(GUEST_TOPIC)
            .user(null)
            .guest(true)
            .build())
        .collect(Collectors.toList());
    return quizRepository.saveAll(quizList);
  }


  @Getter
  @RequiredArgsConstructor
  public enum CreateGuestQuizzesErrorCode implements BaseErrorCode<DomainException> {

    NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
    FAILED_CREATE_OPENAI_REQUEST(HttpStatus.INTERNAL_SERVER_ERROR, "OPENAI 서버 요청 생성에 실패하였습니다."),
    FAILED_CREATE_CHUNK(HttpStatus.INTERNAL_SERVER_ERROR, "텍스트 청크 생성에 실패하였습니다."),
    OPENAI_QUIZ_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OPENAI 서버에서 퀴즈 생성에 실패하였습니다.");

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
  public static class CreateGuestQuizzesRequest implements BaseRequest {

    private String plainText;

    private Quiz.QuizType type;

    @Override
    public boolean isValid() {
      return plainText != null && !plainText.isEmpty() && type != null;
    }
  }

  @Getter
  @Setter
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class CreateGuestQuizzesResponse extends BaseResponse<CreateGuestQuizzesErrorCode> {
    private List<Quiz> quizList;
  }
}