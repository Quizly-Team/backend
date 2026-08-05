package org.quizly.quizly.admin.controller.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.admin.dto.request.CreateDailyQuizRequest;
import org.quizly.quizly.admin.dto.response.CreateDailyQuizResponse;
import org.quizly.quizly.admin.dto.response.CreateDailyQuizResponse.QuestionDetail;
import org.quizly.quizly.admin.service.CreateDailyQuizService;
import org.quizly.quizly.admin.service.CreateDailyQuizService.CreateDailyQuizErrorCode;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.domain.entity.DailyQuiz;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자")
public class CreateDailyQuizController {

    private final CreateDailyQuizService createDailyQuizService;

    @Operation(
        summary = "5분 상식 퀴즈 세트 등록 API",
        description = "관리자 전용 API로 주제별 5분 상식 퀴즈 세트를 등록합니다.\n\n"
            + "- 5분 상식 퀴즈는 OX 문항만 지원합니다.\n"
            + "- `questions`는 정확히 3개이며 `questionNumber`는 1~3을 중복 없이 사용합니다.\n"
            + "- `answer`에 O 또는 X를 전달합니다. 저장 시 TRUE/FALSE로 정규화됩니다.\n"
            + "- `hashtags`는 입력한 문자열이 그대로 저장되며, 문항당 최대 10개까지 저장됩니다.\n"
            + "- `published`를 true로 두면 등록 즉시 발행됩니다. 발행 중인 세트가 여러 개면 "
            + "조회 API가 그중 하나를 무작위로 반환합니다.",
        operationId = "/admin/daily-quizzes"
    )
    @PostMapping("/admin/daily-quizzes")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiErrorCode(errorCodes = {GlobalErrorCode.class, CreateDailyQuizErrorCode.class})
    public ResponseEntity<CreateDailyQuizResponse> createDailyQuiz(
        @RequestBody CreateDailyQuizRequest request
    ) {
        CreateDailyQuizService.CreateDailyQuizResponse serviceResponse = createDailyQuizService.execute(
            CreateDailyQuizService.CreateDailyQuizRequest.builder()
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

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(serviceResponse));
    }

    private CreateDailyQuizResponse toResponse(
        CreateDailyQuizService.CreateDailyQuizResponse serviceResponse) {
        DailyQuiz dailyQuiz = serviceResponse.getDailyQuiz();

        return CreateDailyQuizResponse.builder()
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
