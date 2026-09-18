package org.quizly.quizly.quiz.controller.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.domain.entity.Quiz;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.quizly.quizly.quiz.dto.response.CreateFollowUpQuizResponse;
import org.quizly.quizly.quiz.service.CreateMemberFollowUpQuizService;
import org.quizly.quizly.quiz.service.CreateMemberFollowUpQuizService.CreateMemberFollowUpQuizErrorCode;
import org.quizly.quizly.quiz.service.CreateMemberFollowUpQuizService.CreateMemberFollowUpQuizRequest;
import org.quizly.quizly.quiz.service.CreateMemberFollowUpQuizService.CreateMemberFollowUpQuizResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Quiz", description = "퀴즈")
public class CreateMemberFollowUpQuizController {

    private final CreateMemberFollowUpQuizService createMemberFollowUpQuizService;

    @Operation(
        summary = "회원 꼬리 문제 생성 API",
        description = "선택한 문제(quizId)와 그 문제가 생성될 때 쓰인 원본 텍스트, 지금까지의 꼬리 문제 체인을 바탕으로 "
            + "새로운 꼬리 문제 1개를 생성합니다.\n\n회원 API로 요청 시 토큰이 필요합니다.",
        operationId = "/quizzes/{quizId}/follow-up/member"
    )
    @PostMapping("/quizzes/{quizId}/follow-up/member")
    @ApiErrorCode(errorCodes = {GlobalErrorCode.class, CreateMemberFollowUpQuizErrorCode.class})
    public ResponseEntity<CreateFollowUpQuizResponse> createMemberFollowUpQuiz(
        @PathVariable(name = "quizId") @Schema(description = "꼬리 문제를 생성할 기반 문제 ID", example = "1") Long quizId,
        @AuthenticationPrincipal UserPrincipal userPrincipal) {
        CreateMemberFollowUpQuizResponse serviceResponse = createMemberFollowUpQuizService.execute(
            CreateMemberFollowUpQuizRequest.builder()
                .quizId(quizId)
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

    private CreateFollowUpQuizResponse toResponse(CreateMemberFollowUpQuizResponse serviceResponse) {
        Quiz quiz = serviceResponse.getQuiz();
        return CreateFollowUpQuizResponse.builder()
            .quiz(new CreateFollowUpQuizResponse.QuizDetail(
                quiz.getId(),
                quiz.getParentQuiz() != null ? quiz.getParentQuiz().getId() : null,
                quiz.getQuizText(),
                quiz.getQuizType().name(),
                quiz.getOptions(),
                quiz.getAnswer(),
                quiz.getExplanation(),
                quiz.getTopic()))
            .build();
    }
}
