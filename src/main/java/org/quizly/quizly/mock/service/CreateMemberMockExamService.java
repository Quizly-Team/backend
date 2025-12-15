package org.quizly.quizly.mock.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.core.util.AsyncTaskUtil;
import org.quizly.quizly.core.util.TextProcessingUtil;
import org.quizly.quizly.external.clova.dto.Response.Hcx007MockExamResponse;
import org.quizly.quizly.mock.dto.request.CreateMemberMockExamRequest.MockExamType;
import org.quizly.quizly.mock.service.CreateMemberMockExamService.CreateMemberMockExamRequest;
import org.quizly.quizly.mock.service.CreateMemberMockExamService.CreateMemberMockExamResponse;
import org.quizly.quizly.mock.service.CreateMockQuizService.CreateMockQuizRequest;
import org.quizly.quizly.mock.service.CreateMockQuizService.CreateMockQuizResponse;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class CreateMemberMockExamService implements
    BaseService<CreateMemberMockExamRequest, CreateMemberMockExamResponse> {

  private final CreateMockQuizService createMockQuizService;

  private static final int MIN_QUIZ_COUNT = 10;
  private static final int MAX_QUIZ_COUNT = 30;
  private static final int DEFAULT_MOCK_EXAM_BATCH_SIZE = 2;
  private static final int DEFAULT_CHUNK_SIZE = 500;
  private static final int DEFAULT_CHUNK_OVERLAP = 100;
  private static final int MIN_CHUNK_SIZE = 200;


  @Override
  public CreateMemberMockExamResponse execute(CreateMemberMockExamRequest request) {
    if (request == null || !request.isValid()) {
      return CreateMemberMockExamResponse.builder()
          .success(false)
          .errorCode(CreateMemberMockExamErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }


    List<String> chunkList = request.getChunkList();

    if (chunkList.isEmpty()) {
      log.error("[CreateMemberMockExamService] Failed to create chunks from plainText");
      return CreateMemberMockExamResponse.builder()
          .success(false)
          .errorCode(CreateMemberMockExamErrorCode.FAILED_CREATE_CHUNK)
          .build();
    }
    postProcessingChunkList(chunkList);

    int quizCount = calculateQuizCount(chunkList.size());

    List<CompletableFuture<CreateMockQuizResponse>> createMockQuizResponseFutureList = requestAsyncMockQuizTasks(
        chunkList, request.getMockExamTypeList(), quizCount
    );
    CompletableFuture.allOf(createMockQuizResponseFutureList.toArray(new CompletableFuture[0])).join();
    List<Hcx007MockExamResponse> hcx007MockExamResponseList = AsyncTaskUtil.joinAsyncTasks(
        createMockQuizResponseFutureList, response -> {
          if (response.isSuccess()) {
            return response.getHcx007MockExamResponseList();
          }
          return null;
        });

    if (hcx007MockExamResponseList.isEmpty() || hcx007MockExamResponseList.size() < quizCount) {
      log.error("[CreateMemberMockExamService] Failed to generate any mock exams from Clova Studio after all async.");
      return CreateMemberMockExamResponse.builder()
          .success(false)
          .errorCode(CreateMemberMockExamErrorCode.CLOVA_MOCK_EXAM_GENERATION_FAILED)
          .build();
    }

    postProcessingFindMatch(hcx007MockExamResponseList);

    return CreateMemberMockExamResponse.builder()
        .quizList(hcx007MockExamResponseList)
        .success(true)
        .build();
  }

  private void postProcessingChunkList(List<String> chunkList) {
    if (chunkList.size() <= 1) {
      return;
    }

    int lastIndex = chunkList.size() - 1;
    String lastChunk = chunkList.get(lastIndex);

    if (lastChunk.length() <= MIN_CHUNK_SIZE) {
      if (lastChunk.length() <= DEFAULT_CHUNK_OVERLAP) {
        chunkList.remove(lastIndex);
      } else {
        String targetChunk = chunkList.get(lastIndex - 1);
        chunkList.set(lastIndex - 1, targetChunk + lastChunk.substring(DEFAULT_CHUNK_OVERLAP));
        chunkList.remove(lastIndex);
      }
    }
  }

  private List<CompletableFuture<CreateMockQuizResponse>> requestAsyncMockQuizTasks(
      List<String> chunkList, List<MockExamType> mockExamTypeList, int quizCount) {

    Collections.shuffle(chunkList);
    List<CompletableFuture<CreateMockQuizResponse>> futures = new ArrayList<>();
    int totalTasks = (quizCount + DEFAULT_MOCK_EXAM_BATCH_SIZE - 1) / DEFAULT_MOCK_EXAM_BATCH_SIZE;
    int mockExamTypeListSize = mockExamTypeList.size();
    int chunkListSize = chunkList.size();

    for (int i = 0; i < totalTasks; i++) {
      MockExamType mockExamType = mockExamTypeList.get(i % mockExamTypeListSize);
      String selectedChunk = chunkList.get(i % chunkListSize);

      CreateMockQuizRequest createMockQuizRequest = CreateMockQuizRequest.builder()
          .type(mockExamType)
          .quizCount(DEFAULT_MOCK_EXAM_BATCH_SIZE)
          .plainText(selectedChunk)
          .build();

      CompletableFuture<CreateMockQuizResponse> future = createMockQuizService.execute(createMockQuizRequest);
      futures.add(
          future.exceptionally(ex -> {
            log.error("[CreateMemberMockExamService] Async mock exam creation failed for chunk (length: {})", selectedChunk.length(), ex);
            return null;
          })
      );
    }
    return futures;
  }


  private void postProcessingFindMatch(List<Hcx007MockExamResponse> responseList) {
    responseList.stream()
        .filter(quiz -> quiz.getOptions() != null && !quiz.getOptions().contains(quiz.getAnswer()))
        .forEach(quiz -> {
          if (quiz.getOptions().isEmpty()) {
            quiz.getOptions().add(quiz.getAnswer());
          }
          else {
            quiz.getOptions().set(0, quiz.getAnswer());
          }
        });

    responseList.forEach(quiz -> {
      if (quiz.getOptions() != null) {
        quiz.getOptions().sort((o1, o2) -> {
          int count1 = countItems(o1);
          int count2 = countItems(o2);
          if (count1 != count2) {
            return Integer.compare(count1, count2);
          }
          return o1.compareTo(o2);
        });
      }
    });
  }

  private int countItems(String option) {
    if (option == null) {
      return 0;
    }
    return option.replaceAll("[\\s,]", "").length();
  }

  private int calculateQuizCount(int listSize) {
    int quizCount = listSize * DEFAULT_MOCK_EXAM_BATCH_SIZE;

    if (quizCount < MIN_QUIZ_COUNT) return MIN_QUIZ_COUNT;
    if (quizCount > MAX_QUIZ_COUNT) return MAX_QUIZ_COUNT;
    return quizCount;
  }

  @Getter
  @RequiredArgsConstructor
  public enum CreateMemberMockExamErrorCode implements BaseErrorCode<DomainException> {

    NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
    NOT_EXIST_PROVIDER_ID(HttpStatus.BAD_REQUEST, "Provider ID가 존재하지 않습니다."),
    INVALID_QUIZ_COUNT_RANGE(HttpStatus.BAD_REQUEST, "모의고사 문제 개수는 10개 이상 30개 이하여야 합니다."),
    FAILED_CREATE_CHUNK(HttpStatus.INTERNAL_SERVER_ERROR, "사용자 입력을 chunk 단위 분리에 실패하였습니다."),
    CLOVA_MOCK_EXAM_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CLOVA 서버에서 모의고사 생성에 실패하였습니다.");

    private final HttpStatus httpStatus;

    private final String message;

    @Override
    public DomainException toException() {
      return new DomainException(httpStatus, this);
    }
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class CreateMemberMockExamRequest implements BaseRequest {

    private List<String> chunkList;

    private List<MockExamType> mockExamTypeList;

    private UserPrincipal userPrincipal;

    private Integer quizCount;

    @Override
    public boolean isValid() {
      return chunkList != null && !chunkList.isEmpty() && mockExamTypeList != null
          && !mockExamTypeList.isEmpty() && userPrincipal != null;
    }
  }

  @Getter
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class CreateMemberMockExamResponse extends
      BaseResponse<CreateMemberMockExamErrorCode> {

    private List<Hcx007MockExamResponse> quizList;
  }
}