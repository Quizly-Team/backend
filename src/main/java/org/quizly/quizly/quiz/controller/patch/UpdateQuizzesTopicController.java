package org.quizly.quizly.quiz.controller.patch;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.quizly.quizly.quiz.dto.request.UpdateQuizzesTopicRequest;
import org.quizly.quizly.quiz.dto.response.UpdateQuizzesTopicResponse;
import org.quizly.quizly.quiz.service.UpdateQuizzesTopicService;
import org.quizly.quizly.quiz.service.UpdateQuizzesTopicService.UpdateQuizzesTopicErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Quiz", description = "퀴즈")
public class UpdateQuizzesTopicController {

  private final UpdateQuizzesTopicService updateQuizzesTopicService;

  @Operation(
      summary = "문제의 주제 변경 API",
      description = "회원 전용 API로 원하는 문제의 주제를 변경합니다.\n\n회원 API로 요청 시 토큰이 필요합니다.",
      operationId = "/quizzes/topic"
  )
  @PatchMapping("/quizzes/topic")
  @ApiErrorCode(errorCodes = {GlobalErrorCode.class, UpdateQuizzesTopicErrorCode.class})
  public ResponseEntity<UpdateQuizzesTopicResponse> updateQuizzesTopic (
    @RequestBody UpdateQuizzesTopicRequest request,
    @AuthenticationPrincipal UserPrincipal userPrincipal
  ) {
    UpdateQuizzesTopicService.UpdateQuizzesTopicResponse serviceResponse = updateQuizzesTopicService.execute(
        UpdateQuizzesTopicService.UpdateQuizzesTopicRequest.builder()
            .topic(request.getTopic())
            .quizIdList(request.getQuizIdList())
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

    return ResponseEntity.ok(UpdateQuizzesTopicResponse.builder().build());
  }
}
