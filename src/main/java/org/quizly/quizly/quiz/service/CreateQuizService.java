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
import org.quizly.quizly.core.domin.entity.Quiz;
import org.quizly.quizly.core.domin.repository.QuizRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.external.clova.service.CreateQuizClovaStudioService;
import org.quizly.quizly.external.clova.dto.Response.Hcx007Response;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class CreateQuizService implements BaseService<CreateQuizService.CreateQuizRequest, CreateQuizService.CreateQuizResponse> {

  private final CreateQuizClovaStudioService createQuizClovaStudioService;
  private final QuizRepository quizRepository;

  @Override
  public CreateQuizResponse execute(CreateQuizRequest request) {
    if (request == null || !request.isValid()) {
      return CreateQuizResponse.builder()
          .success(false)
          .errorCode(CreateQuizErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    CreateQuizClovaStudioService.CreateQuizClovaStudioResponse clovaResponse = createQuizClovaStudioService.execute(
        CreateQuizClovaStudioService.CreateQuizClovaStudioRequest.builder()
            .plainText(request.getPlainText())
            .type(request.getType())
            .build()
    );

    if (!clovaResponse.isSuccess()) {
      log.error("[CreateQuizService] Failed to create quiz from Clova Studio. Error: {}", clovaResponse.getErrorCode());
      return CreateQuizResponse.builder()
          .success(false)
          .errorCode(CreateQuizErrorCode.FAILED_CREATE_CLOVA_REQUEST)
          .build();
    }

    List<Hcx007Response> hcx007ResponseList = clovaResponse.getHcx007Responses();
    if (hcx007ResponseList == null || hcx007ResponseList.isEmpty()) {
      log.info("[CreateQuizService] No quizzes were generated from Clova Studio.");
      return CreateQuizResponse.builder()
          .success(false)
          .errorCode(CreateQuizErrorCode.CLOVA_QUIZ_GENERATION_FAILED)
          .build();
    }

    List<Quiz> quizList = saveQuiz(hcx007ResponseList);

    return CreateQuizResponse.builder().quizList(quizList).build();
  }

  private List<Quiz> saveQuiz(List<Hcx007Response> hcx007ResponseList) {
    Boolean guest = true;
    List<Quiz> quizList = hcx007ResponseList.stream()
        .map(hcx007Response -> Quiz.builder()
            .quizText(hcx007Response.getQuiz())
            .answer(hcx007Response.getAnswer())
            .quizType(hcx007Response.getType())
            .explanation(hcx007Response.getExplanation())
            .options(hcx007Response.getOptions())
            .topic(hcx007Response.getTopic())
            .user(null)
            .guest(guest)
            .build())
        .collect(Collectors.toList());
    return quizRepository.saveAll(quizList);
  }

  @Getter
  @RequiredArgsConstructor
  public enum CreateQuizErrorCode implements BaseErrorCode<DomainException> {

    NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
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
  public static class CreateQuizRequest implements BaseRequest {

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
  public static class CreateQuizResponse extends BaseResponse<CreateQuizErrorCode> {
    private List<Quiz> quizList;
  }
}