package org.quizly.quizly.mock.service;

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
import org.quizly.quizly.core.application.BaseAsyncService;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.ResponseFormat;
import org.quizly.quizly.external.clova.dto.Response.Hcx007MockExamResponse;
import org.quizly.quizly.external.clova.service.CreateMockExamClovaStudioService;
import org.quizly.quizly.external.clova.service.CreateMockExamClovaStudioService.CreateMockExamClovaStudioRequest;
import org.quizly.quizly.external.clova.service.CreateMockExamClovaStudioService.CreateMockExamClovaStudioResponse;
import org.quizly.quizly.external.clova.util.ResponseFormatUtil;
import org.quizly.quizly.mock.dto.request.CreateMemberMockExamRequest.MockExamType;
import org.quizly.quizly.mock.dto.request.CreateMemberMockExamRequest.MockExamType.TypeCategory;
import org.quizly.quizly.mock.service.CreateMockQuizService.CreateMockQuizRequest;
import org.quizly.quizly.mock.service.CreateMockQuizService.CreateMockQuizResponse;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class CreateMockQuizService implements BaseAsyncService<CreateMockQuizRequest, CreateMockQuizResponse> {

  private final CreateMockExamClovaStudioService createMockExamClovaStudioService;

  @Async("mockExamTaskExecutor")
  @Override
  public CompletableFuture<CreateMockQuizResponse> execute(CreateMockQuizRequest request) {
    if (request == null || !request.isValid()) {
      return CompletableFuture.completedFuture(CreateMockQuizResponse.builder()
          .success(false)
          .errorCode(CreateMockQuizErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build());
    }

    String promptPath = request.getType().getPromptPath();
    if (promptPath == null) {
      log.error("[CreateMockQuizService] Could not find a prompt path. MockExamType: {}", request.getType());
      return CompletableFuture.completedFuture(CreateMockQuizResponse.builder()
          .success(false)
          .errorCode(CreateMockQuizErrorCode.NOT_EXIST_PROMPT_PATH)
          .build());
    }

    ResponseFormat responseFormat = createResponseFormat(request);

    CreateMockExamClovaStudioResponse clovaResponse = createMockExamClovaStudioService.execute(
        CreateMockExamClovaStudioRequest.builder()
            .plainText(request.getPlainText())
            .promptPath(promptPath)
            .responseFormat(responseFormat)
            .build());

    if (clovaResponse != null && clovaResponse.isSuccess()) {
      return CompletableFuture.completedFuture(CreateMockQuizResponse.builder()
          .success(true)
          .hcx007MockExamResponseList(clovaResponse.getHcx007MockExamResponse())
          .build());
    }

    log.error("[CreateMockQuizService] Thread: {}. Failed to create mock exam from Clova Studio.", Thread.currentThread().getName());
    return CompletableFuture.completedFuture(CreateMockQuizResponse.builder()
        .success(false)
        .errorCode(CreateMockQuizErrorCode.ASYNC_FAILED_CLOVA_MOCK_EXAM_GENERATION)
        .build());
  }

  private ResponseFormat createResponseFormat(CreateMockQuizRequest request) {
    int quizCount = request.getQuizCount();
    TypeCategory category = request.getType().getTypeCategory();

    if (category == TypeCategory.DESCRIPTIVE) {
      return ResponseFormatUtil.createDescriptiveQuizResponseFormat(quizCount);
    } else {
      return ResponseFormatUtil.createSelectionQuizResponseFormat(quizCount);
    }
  }

  @Getter
  @RequiredArgsConstructor
  public enum CreateMockQuizErrorCode implements BaseErrorCode<DomainException> {

    NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
    NOT_EXIST_PROMPT_PATH(HttpStatus.INTERNAL_SERVER_ERROR, "요청 유형의 프롬프트 주소가 존재하지 않습니다."),
    ASYNC_FAILED_CLOVA_MOCK_EXAM_GENERATION(HttpStatus.INTERNAL_SERVER_ERROR, "CLOVA 서버의 비동기 모의고사 생성 중 실패하였습니다.");

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
  public static class CreateMockQuizRequest implements BaseRequest {
    private MockExamType type;
    private int quizCount;
    private String plainText;

    @Override
    public boolean isValid() {
      return type != null && plainText != null && !plainText.isEmpty() && quizCount > 0;
    }
  }

  @Getter
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class CreateMockQuizResponse extends BaseResponse<CreateMockQuizErrorCode>  {
    private List<Hcx007MockExamResponse> hcx007MockExamResponseList;
  }
}
