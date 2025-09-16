package org.quizly.quizly.quiz.controller.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.domin.entity.Quiz;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.quiz.dto.request.CreateQuizzesRequest;
import org.quizly.quizly.quiz.dto.response.CreateQuizzesResponse;
import org.quizly.quizly.quiz.service.CreateGuestQuizzesService;
import org.quizly.quizly.quiz.service.CreateGuestQuizzesService.CreateGuestQuizzesErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Quiz", description = "퀴즈")
public class CreateGuestQuizzesController {

  private final CreateGuestQuizzesService createGuestQuizzesService;

  @Operation(
      summary = "텍스트 기반 비회원 퀴즈 생성 API",
      description = "비회원 전용 API로 3문제 생성 합니다.\n\n비회원 API로 토큰 없이 호출합니다.\n\n텍스트 기반 OX/객관식 퀴즈를 제작합니다.",
      operationId = "/quizzes/guest"
  )
  @PostMapping("/quizzes/guest")
  @ApiErrorCode(errorCodes = {GlobalErrorCode.class, CreateGuestQuizzesErrorCode.class})
  public ResponseEntity<CreateQuizzesResponse> createGuestQuizzes(
      @RequestBody CreateQuizzesRequest request) {
    CreateGuestQuizzesService.CreateGuestQuizzesResponse serviceResponse = createGuestQuizzesService.execute(
        CreateGuestQuizzesService.CreateGuestQuizzesRequest.builder()
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

  private CreateQuizzesResponse toResponse(CreateGuestQuizzesService.CreateGuestQuizzesResponse serviceResponse) {
    List<Quiz> quizList = serviceResponse.getQuizList();
    List<CreateQuizzesResponse.QuizDetail> quizDetailList = quizList.stream()
        .map(quiz -> new CreateQuizzesResponse.QuizDetail(
            quiz.getId(),
            quiz.getQuizText(),
            quiz.getQuizType().name(),
            quiz.getOptions(),
            quiz.getAnswer(),
            quiz.getExplanation(),
            quiz.getTopic()))
        .toList();
    return CreateQuizzesResponse.builder()
        .quizDetailList(quizDetailList)
        .build();
  }
}
