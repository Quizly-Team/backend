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
import org.quizly.quizly.quiz.service.GradeMemberWrongQuizzesService;
import org.quizly.quizly.quiz.service.GradeMemberWrongQuizzesService.GradeMemberWrongQuizzesErrorCode;
import org.quizly.quizly.quiz.service.GradeMemberWrongQuizzesService.GradeMemberWrongQuizzesRequest;
import org.quizly.quizly.quiz.service.GradeMemberWrongQuizzesService.GradeMemberWrongQuizzesResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Quiz", description = "퀴즈")
public class GradeMemberWrongQuizzesController {

  private final GradeMemberWrongQuizzesService gradeMemberWrongQuizzesService;

  @Operation(
      summary = "회원 오답 노트 문제 채점 API",
      description = "오답 노트에서 틀린 문제를 다시 풀 때 사용하는 API입니다.\n\n"
          + "답변을 제출하면 문제를 채점하고 새로운 풀이 기록을 저장합니다.\n\n"
          + "주의: 처음 문제를 풀 때는 /quizzes/{quizId}/answer/member API를 사용하세요.\n\n"
          + "회원 API로 요청 시 토큰이 필요합니다.\n\n"
          + "userAnswer : 객관식의 경우 TRUE/FALSE, 주관식의 경우 String 형식으로 답변을 입력받습니다.",
      operationId = "/quizzes/{quizId}/answer/retry"
  )
  @PostMapping("/quizzes/{quizId}/answer/retry")
  @ApiErrorCode(errorCodes = {GlobalErrorCode.class, GradeMemberWrongQuizzesErrorCode.class})
  public ResponseEntity<GradeQuizzesResponse> gradeMemberWrongQuizzes(
      @PathVariable(name = "quizId") @Schema(description = "문제 ID", example = "1") Long quizId,
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestBody GradeQuizzesRequest request) {
    GradeMemberWrongQuizzesResponse serviceResponse = gradeMemberWrongQuizzesService.execute(
        GradeMemberWrongQuizzesRequest.builder()
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