package org.quizly.quizly.quiz.controller.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.domin.entity.Quiz;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.quiz.dto.request.GradeQuizzesRequest;
import org.quizly.quizly.quiz.dto.response.GradeQuizzesResponse;
import org.quizly.quizly.quiz.service.GradeGuestQuizzesService;
import org.quizly.quizly.quiz.service.GradeGuestQuizzesService.GradeGuestQuizzesErrorCode;
import org.quizly.quizly.quiz.service.GradeGuestQuizzesService.GradeGuestQuizzesRequest;
import org.quizly.quizly.quiz.service.GradeGuestQuizzesService.GradeGuestQuizzesResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Quiz", description = "퀴즈")
public class GradeGuestQuizzesController {

  private final GradeGuestQuizzesService gradeGuestQuizzesService;

  @Operation(
      summary = "비회원 문제 채점 API",
      description = "비회원 전용 API로 답변을 제출하면 문제를 채점합니다.\n\n비회원 API로 토큰 없이 호출합니다.\n\nuserAnswer : 객관식의 경우 TRUE/FALSE, 주관식의 경우 String 형식으로 답변을 입력받습니다.",
      operationId = "/quizzes/{quizId}/answer/guest"
  )
  @PostMapping("/quizzes/{quizId}/answer/guest")
  @ApiErrorCode(errorCodes = {GlobalErrorCode.class, GradeGuestQuizzesErrorCode.class})
  public ResponseEntity<GradeQuizzesResponse> gradeGuestQuizzes(
      @PathVariable(name = "quizId") @Schema(description = "문제 ID", example = "1") Long quizId,
      @RequestBody GradeQuizzesRequest request) {
    GradeGuestQuizzesResponse serviceResponse = gradeGuestQuizzesService.execute(
        GradeGuestQuizzesRequest.builder()
            .quizId(quizId)
            .userAnswer(request.getUserAnswer())
            .build()
    );

    if (serviceResponse == null || !serviceResponse.isSuccess()) {
      Optional.ofNullable(serviceResponse)
          .map(BaseResponse::getErrorCode)
          .ifPresentOrElse(errorCode -> {
            throw errorCode.toException();
          }, () -> {
            throw GlobalErrorCode.INTERNAL_ERROR.toException();
          });
    }

    Quiz quiz = serviceResponse.getQuiz();
    if (quiz == null) {
      throw GlobalErrorCode.INTERNAL_ERROR.toException();
    }

    return ResponseEntity.ok(
        GradeQuizzesResponse.builder()
            .quizId(quiz.getId())
            .isCorrect(serviceResponse.isCorrect())
            .Answer(quiz.getAnswer())
            .explanation(quiz.getExplanation())
            .build());
  }
}
