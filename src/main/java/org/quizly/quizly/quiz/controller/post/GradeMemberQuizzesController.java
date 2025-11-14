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
import org.quizly.quizly.oauth.UserPrincipal;
import org.quizly.quizly.quiz.dto.request.GradeQuizzesRequest;
import org.quizly.quizly.quiz.dto.response.GradeQuizzesResponse;
import org.quizly.quizly.quiz.service.GradeMemberQuizzesService;
import org.quizly.quizly.quiz.service.GradeMemberQuizzesService.GradeMemberQuizzesErrorCode;
import org.quizly.quizly.quiz.service.GradeMemberQuizzesService.GradeMemberQuizzesRequest;
import org.quizly.quizly.quiz.service.GradeMemberQuizzesService.GradeMemberQuizzesResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Quiz", description = "퀴즈")
public class GradeMemberQuizzesController {

  private final GradeMemberQuizzesService gradeMemberQuizzesService;

  @Operation(
      summary = "회원 문제 채점 API",
      description = "회원 전용 API로 답변을 제출하면 문제를 채점, 풀이 기록을 저장합니다.\n\n회원 API로 요청 시 토큰이 필요합니다.\n\nuserAnswer : 객관식의 경우 TRUE/FALSE, 주관식의 경우 String 형식으로 답변을 입력받습니다.",
      operationId = "/quizzes/{quizId}/answer/member"
  )
  @PostMapping("/quizzes/{quizId}/answer/member")
  @ApiErrorCode(errorCodes = {GlobalErrorCode.class, GradeMemberQuizzesErrorCode.class})
  public ResponseEntity<GradeQuizzesResponse> gradeMemberQuizzes(
      @PathVariable(name = "quizId") @Schema(description = "문제 ID", example = "1") Long quizId,
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody GradeQuizzesRequest request) {
    GradeMemberQuizzesResponse serviceResponse = gradeMemberQuizzesService.execute(
        GradeMemberQuizzesRequest.builder()
            .quizId(quizId)
            .userAnswer(request.getUserAnswer())
                .solveTime(request.getSolveTime())
            .userPrincipal(userPrincipal)
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
            .answer(quiz.getAnswer())
            .explanation(quiz.getExplanation())
            .build());
  }
}

