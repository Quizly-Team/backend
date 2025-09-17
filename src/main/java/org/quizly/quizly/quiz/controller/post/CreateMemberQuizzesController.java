package org.quizly.quizly.quiz.controller.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.domin.entity.Quiz;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.quizly.quizly.quiz.dto.request.CreateQuizzesRequest;
import org.quizly.quizly.quiz.dto.response.CreateQuizzesResponse;
import org.quizly.quizly.quiz.service.CreateMemberQuizzesService;
import org.quizly.quizly.quiz.service.CreateMemberQuizzesService.CreateMemberQuizzesErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Quiz", description = "퀴즈")
public class CreateMemberQuizzesController {

  private final CreateMemberQuizzesService createMemberQuizzesService;

  @Operation(
      summary = "텍스트 기반 회원 퀴즈 생성 API",
      description = "회원 전용 API로 10문제 생성 합니다.\n\n회원 API로 요청 시 토큰이 필요합니다.\n\n텍스트 기반 OX/객관식 퀴즈를 제작합니다.",
      operationId = "/quizzes/member"
  )
  @PostMapping("/quizzes/member")
  @ApiErrorCode(errorCodes = {GlobalErrorCode.class, CreateMemberQuizzesErrorCode.class})
  public ResponseEntity<CreateQuizzesResponse> createMemberQuizzes(
      @RequestBody CreateQuizzesRequest request,
      @AuthenticationPrincipal UserPrincipal userPrincipal) {
    CreateMemberQuizzesService.CreateMemberQuizzesResponse serviceResponse = createMemberQuizzesService.execute(
        CreateMemberQuizzesService.CreateMemberQuizzesRequest.builder()
            .plainText(request.getPlainText())
            .type(request.getType())
            .userPrincipal(userPrincipal)
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

  private CreateQuizzesResponse toResponse(CreateMemberQuizzesService.CreateMemberQuizzesResponse serviceResponse) {
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

