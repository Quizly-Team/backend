package org.quizly.quizly.quiz.controller.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.domin.entity.Quiz;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.quiz.dto.request.CreateQuizRequest;
import org.quizly.quizly.quiz.dto.response.CreateQuizResponse;
import org.quizly.quizly.quiz.service.CreateQuizService;
import org.quizly.quizly.quiz.service.CreateQuizService.CreateQuizErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "quiz", description = "퀴즈")
public class CreateQuizController {

  private final CreateQuizService createQuizService;

  @Operation(
      summary = "퀴즈 생성 API",
      description = "OX/객관식 문제를 제작합니다.",
      operationId = "/quiz/create"
  )
  @PostMapping("/quiz/create")
  @ApiErrorCode(errorCodes = {GlobalErrorCode.class, CreateQuizErrorCode.class})
  public ResponseEntity<CreateQuizResponse> createQuiz(
      @RequestBody CreateQuizRequest request) {
    CreateQuizService.CreateQuizResponse serviceResponse = createQuizService.execute(
        CreateQuizService.CreateQuizRequest.builder()
            .plainText(request.getPlainText())
            .type(request.getType())
            .build());

    if (serviceResponse == null || !serviceResponse.isSuccess()) {
      Optional.ofNullable(serviceResponse)
          .map(BaseResponse::getErrorCode)
          .ifPresentOrElse(errorCode -> {
            throw errorCode.toException();
          }, () -> {
            throw GlobalErrorCode.INTERNAL_ERROR.toException();
          });
    }

    return ResponseEntity.ok(toResponse(serviceResponse));
  }

  private CreateQuizResponse toResponse(CreateQuizService.CreateQuizResponse serviceResponse) {
    List<Quiz> quizList = serviceResponse.getQuizList();
    List<CreateQuizResponse.QuizDetail> quizDetailList = quizList.stream()
        .map(quiz -> new CreateQuizResponse.QuizDetail(
            quiz.getId(),
            quiz.getQuizText(),
            quiz.getQuizType().name(),
            quiz.getOptions(),
            quiz.getAnswer(),
            quiz.getExplanation(),
            quiz.getTopic()))
        .toList();
    return CreateQuizResponse.builder()
        .quizDetailList(quizDetailList)
        .build();
  }
}
