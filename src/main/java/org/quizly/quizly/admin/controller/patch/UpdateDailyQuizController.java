package org.quizly.quizly.admin.controller.patch;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.admin.dto.request.UpdateDailyQuizRequest;
import org.quizly.quizly.admin.dto.response.UpdateDailyQuizResponse;
import org.quizly.quizly.admin.dto.response.UpdateDailyQuizResponse.QuestionDetail;
import org.quizly.quizly.admin.service.UpdateDailyQuizService;
import org.quizly.quizly.admin.service.UpdateDailyQuizService.UpdateDailyQuizErrorCode;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.domain.entity.DailyQuiz;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자")
public class UpdateDailyQuizController {

    private final UpdateDailyQuizService updateDailyQuizService;

    @Operation(
        summary = "5분 상식 퀴즈 세트 수정/발행 API",
        description = "관리자 전용 API로 5분 상식 퀴즈 세트의 내용을 수정하거나 발행 상태를 전환합니다.\n\n"
            + "- 전달한 필드만 반영되며, 생략한 필드는 기존 값을 유지합니다.\n"
            + "- 발행 전환만 하려면 `{\"published\": true}` 만 전달하면 됩니다.\n"
            + "- `questions`를 전달하면 기존 문항을 통째로 교체합니다(최소 1개 이상). "
            + "생략하면 기존 문항이 그대로 유지됩니다.\n"
            + "- `questions`를 전달할 때 `hashtags`는 입력한 문자열이 그대로 저장되며, "
            + "문항당 최대 10개까지 저장됩니다.\n"
            + "- 발행 중인 세트가 여러 개면 조회 API가 그중 하나를 무작위로 반환합니다.",
        operationId = "/admin/daily-quizzes/{dailyQuizId}"
    )
    @PatchMapping("/admin/daily-quizzes/{dailyQuizId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiErrorCode(errorCodes = {GlobalErrorCode.class, UpdateDailyQuizErrorCode.class})
    public ResponseEntity<UpdateDailyQuizResponse> updateDailyQuiz(
        @PathVariable @Schema(description = "5분 상식 퀴즈 세트 ID", example = "1") Long dailyQuizId,
        @RequestBody UpdateDailyQuizRequest request
    ) {
        UpdateDailyQuizService.UpdateDailyQuizResponse serviceResponse = updateDailyQuizService.execute(
            UpdateDailyQuizService.UpdateDailyQuizRequest.builder()
                .dailyQuizId(dailyQuizId)
                .topic(request.getTopic())
                .warmUp(request.getWarmUp())
                .sourceContent(request.getSourceContent())
                .published(request.getPublished())
                .questions(request.toQuestionCommands())
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

    private UpdateDailyQuizResponse toResponse(
        UpdateDailyQuizService.UpdateDailyQuizResponse serviceResponse) {
        DailyQuiz dailyQuiz = serviceResponse.getDailyQuiz();

        return UpdateDailyQuizResponse.builder()
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
            .build();
    }
}
