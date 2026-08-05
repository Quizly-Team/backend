package org.quizly.quizly.dailyquiz.controller.get;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.domain.entity.DailyQuiz;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.dailyquiz.dto.response.ReadDailyQuizResponse;
import org.quizly.quizly.dailyquiz.dto.response.ReadDailyQuizResponse.QuestionDetail;
import org.quizly.quizly.dailyquiz.service.ReadDailyQuizService;
import org.quizly.quizly.dailyquiz.service.ReadDailyQuizService.ReadDailyQuizErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "DailyQuiz", description = "5분 상식 퀴즈")
public class ReadDailyQuizController {

    private final ReadDailyQuizService readDailyQuizService;

    @Operation(
        summary = "5분 상식 퀴즈 조회 API",
        description = "발행 중인 5분 상식 퀴즈 세트 중 하나를 무작위로 조회합니다.\n\n"
            + "- OX 문항이 `questionNumber` 오름차순으로 3개 반환됩니다.\n"
            + "- 정답(`answer`)과 해설(`explanation`)이 함께 내려가므로 프론트에서 즉시 채점할 수 있습니다.\n"
            + "- `answer`는 TRUE 또는 FALSE입니다.\n"
            + "- 발행 중인 세트가 하나도 없으면 404를 반환합니다.",
        operationId = "/daily-quizzes"
    )
    @GetMapping("/daily-quizzes")
    @ApiErrorCode(errorCodes = {GlobalErrorCode.class, ReadDailyQuizErrorCode.class})
    public ResponseEntity<ReadDailyQuizResponse> readDailyQuiz() {
        ReadDailyQuizService.ReadDailyQuizResponse serviceResponse = readDailyQuizService.execute(
            ReadDailyQuizService.ReadDailyQuizRequest.builder().build()
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

    private ReadDailyQuizResponse toResponse(
        ReadDailyQuizService.ReadDailyQuizResponse serviceResponse) {
        DailyQuiz dailyQuiz = serviceResponse.getDailyQuiz();

        return ReadDailyQuizResponse.builder()
            .dailyQuizId(dailyQuiz.getId())
            .topic(dailyQuiz.getTopic())
            .warmUp(dailyQuiz.getWarmUp())
            .sourceContent(dailyQuiz.getSourceContent())
            .questions(dailyQuiz.getQuestions().stream()
                .map(question -> new QuestionDetail(
                    question.getId(),
                    question.getQuestionNumber(),
                    question.getQuestionText(),
                    question.getAnswer(),
                    question.getExplanation()))
                .toList())
            .build();
    }
}
