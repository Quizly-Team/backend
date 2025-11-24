package org.quizly.quizly.quiz.service;

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
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.external.clova.service.CreateTopicClovaStudioService;
import org.quizly.quizly.external.clova.service.CreateTopicClovaStudioService.CreateTopicClovaStudioRequest;
import org.quizly.quizly.external.clova.service.CreateTopicClovaStudioService.CreateTopicClovaStudioResponse;
import org.quizly.quizly.quiz.service.CreateTopicService.CreateTopicRequest;
import org.quizly.quizly.quiz.service.CreateTopicService.CreateTopicResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class CreateTopicService implements BaseService<CreateTopicRequest, CreateTopicResponse> {

  private final CreateTopicClovaStudioService createTopicClovaStudioService;

  private static final String TOPIC_PROMPT_PATH = "prompt/basic_quiz/topic.txt";

  @Override
  public CreateTopicResponse execute(CreateTopicRequest request) {
    if (request == null || !request.isValid()) {
      return CreateTopicResponse.builder()
          .success(false)
          .errorCode(CreateTopicErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    CreateTopicClovaStudioResponse clovaResponse = createTopicClovaStudioService.execute(
        CreateTopicClovaStudioRequest.builder()
            .plainText(request.getPlainText())
            .promptPath(TOPIC_PROMPT_PATH)
            .build()
    );

    if (clovaResponse == null || !clovaResponse.isSuccess()) {
      log.error("[CreateTopicService] Failed to create topic from Clova Studio. Response: {}", clovaResponse);
      return CreateTopicResponse.builder()
          .success(false)
          .errorCode(CreateTopicErrorCode.FAILED_CREATE_TOPIC)
          .build();
    }

    String topic = clovaResponse.getTopic();
    if (topic == null || topic.isBlank()) {
      log.error("[CreateTopicService] Topic is empty from Clova response");
      return CreateTopicResponse.builder()
          .success(false)
          .errorCode(CreateTopicErrorCode.EMPTY_TOPIC)
          .build();
    }

    return CreateTopicResponse.builder()
        .topic(topic)
        .success(true)
        .build();
  }

  @Getter
  @RequiredArgsConstructor
  public enum CreateTopicErrorCode implements BaseErrorCode<DomainException> {

    NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
    FAILED_CREATE_TOPIC(HttpStatus.INTERNAL_SERVER_ERROR, "주제 생성에 실패하였습니다."),
    EMPTY_TOPIC(HttpStatus.INTERNAL_SERVER_ERROR, "생성된 주제가 비어있습니다.");

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
  public static class CreateTopicRequest implements BaseRequest {

    private String plainText;

    @Override
    public boolean isValid() {
      return plainText != null && !plainText.isBlank();
    }
  }

  @Getter
  @Setter
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class CreateTopicResponse extends BaseResponse<CreateTopicErrorCode> {
    private String topic;
  }
}
