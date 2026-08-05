package org.quizly.quizly.admin.controller.get;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.admin.dto.response.AdminReadDailyQuizResponse;
import org.quizly.quizly.admin.dto.response.AdminReadDailyQuizResponse.QuestionDetail;
import org.quizly.quizly.admin.service.AdminReadDailyQuizService;
import org.quizly.quizly.admin.service.AdminReadDailyQuizService.AdminReadDailyQuizErrorCode;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.domain.entity.DailyQuiz;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자")
public class AdminReadDailyQuizController {

    private final AdminReadDailyQuizService adminReadDailyQuizService;

    @Operation(
        summary = "5분 상식 퀴즈 세트 상세 조회 API",
        description = "관리자 전용 API로 5분 상식 퀴즈 세트 1건의 전체 내용을 조회합니다.\n\n"
            + "- 수정 화면에서 현재 값을 채우기 위한 API로, 원본 자료와 전체 문항을 모두 포함합니다.\n"
            + "- 미발행 세트도 조회할 수 있습니다.\n"
            + "- 문항은 `questionNumber` 오름차순으로 반환됩니다.",
        operationId = "/admin/daily-quizzes/{dailyQuizId}"
    )
    @GetMapping("/admin/daily-quizzes/{dailyQuizId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiErrorCode(errorCodes = {GlobalErrorCode.class, AdminReadDailyQuizErrorCode.class})
    public ResponseEntity<AdminReadDailyQuizResponse> adminReadDailyQuiz(
        @PathVariable @Schema(description = "5분 상식 퀴즈 세트 ID", example = "1") Long dailyQuizId
    ) {
        AdminReadDailyQuizService.AdminReadDailyQuizResponse serviceResponse =
            adminReadDailyQuizService.execute(
                AdminReadDailyQuizService.AdminReadDailyQuizRequest.builder()
                    .dailyQuizId(dailyQuizId)
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

        return ResponseEntity.ok(toResponse(serviceResponse));
    }

    private AdminReadDailyQuizResponse toResponse(
        AdminReadDailyQuizService.AdminReadDailyQuizResponse serviceResponse) {
        DailyQuiz dailyQuiz = serviceResponse.getDailyQuiz();

        return AdminReadDailyQuizResponse.builder()
            .dailyQuizId(dailyQuiz.getId())
            .topic(dailyQuiz.getTopic())
            .published(dailyQuiz.getPublished())
            .warmUp(dailyQuiz.getWarmUp())
            .sourceContent(dailyQuiz.getSourceContent())
            .questions(dailyQuiz.getQuestions().stream()
                .map(question -> new QuestionDetail(
                    question.getId(),
                    question.getQuestionNumber(),
                    question.getQuestionText(),
                    question.getAnswer(),
                    question.getExplanation(),
                    question.getHashtags()))
                .toList())
            .createdAt(dailyQuiz.getCreatedAt())
            .updatedAt(dailyQuiz.getUpdatedAt())
            .build();
    }
}
