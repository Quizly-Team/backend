package org.quizly.quizly.quiz.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.core.application.BaseAsyncService;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.domin.entity.Quiz;
import org.quizly.quizly.core.domin.entity.Quiz.QuizType;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.external.openai.dto.Request.OpenAiRequest.ResponseFormat;
import org.quizly.quizly.external.openai.service.CreateQuizOpenAiService;
import org.quizly.quizly.external.openai.service.CreateQuizOpenAiService.CreateQuizOpenAiRequest;
import org.quizly.quizly.external.openai.service.CreateQuizOpenAiService.CreateQuizOpenAiResponse;
import org.quizly.quizly.external.openai.util.OpenAiResponseFormatUtil;
import org.quizly.quizly.quiz.dto.response.GeneratedQuizResponse;
import org.quizly.quizly.quiz.service.CreateQuizService.CreateQuizRequest;
import org.quizly.quizly.quiz.service.CreateQuizService.CreateQuizResponse;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class CreateQuizService implements BaseAsyncService<CreateQuizRequest, CreateQuizResponse> {

    private final CreateQuizOpenAiService createQuizOpenAiService;

    @Async("quizTaskExecutor")
    @Override
    public CompletableFuture<CreateQuizResponse> execute(CreateQuizRequest request) {
        if (request == null || !request.isValid()) {
            return CompletableFuture.completedFuture(CreateQuizResponse.builder()
                .success(false)
                .errorCode(CreateQuizErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
                .build());
        }
        String promptPath = request.getType().getPromptPath();
        if (promptPath == null) {
            log.error("[CreateQuizService] Could not find a prompt path. QuizType: {}",
                request.getType());
            return CompletableFuture.completedFuture(CreateQuizResponse.builder()
                .success(false)
                .errorCode(CreateQuizErrorCode.NOT_EXIST_PROMPT_PATH)
                .build());
        }

        ResponseFormat responseFormat = createResponseFormat(request);

        CreateQuizOpenAiResponse openAiResponse = createQuizOpenAiService.execute(
            CreateQuizOpenAiRequest.builder()
                .plainText(request.getPlainText())
                .promptPath(promptPath)
                .responseFormat(responseFormat)
                .build());

        if (openAiResponse != null && openAiResponse.isSuccess()) {
            return CompletableFuture.completedFuture(CreateQuizResponse.builder()
                .success(true)
                .generatedQuizResponseList(openAiResponse.getGeneratedQuizResponseList())
                .build());
        }

        log.error("[CreateQuizService] Thread: {}. Failed to create quiz from OpenAi.",
            Thread.currentThread().getName());
        return CompletableFuture.completedFuture(CreateQuizResponse.builder()
            .success(false)
            .errorCode(CreateQuizErrorCode.ASYNC_FAILED_OPENAI_QUIZ_GENERATION)
            .build());
    }

    private ResponseFormat createResponseFormat(CreateQuizRequest request) {
        int quizCount = request.getQuizCount();
        String quizType = request.getType().name();

        if (request.getType().equals(QuizType.TRUE_FALSE)) {
            return OpenAiResponseFormatUtil.createDescriptiveQuizResponseFormat(quizCount,
                quizType);
        } else {
            return OpenAiResponseFormatUtil.createSelectionQuizResponseFormat(quizCount, quizType);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public enum CreateQuizErrorCode implements BaseErrorCode<DomainException> {

        NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
        NOT_EXIST_PROMPT_PATH(HttpStatus.INTERNAL_SERVER_ERROR, "요청 유형의 프롬프트 주소가 존재하지 않습니다."),
        ASYNC_FAILED_OPENAI_QUIZ_GENERATION(HttpStatus.INTERNAL_SERVER_ERROR,
            "OPENAI 서버의 비동기 퀴즈 생성 중 실패하였습니다.");

        private final HttpStatus httpStatus;

        private final String message;

        @Override
        public DomainException toException() {
            return new DomainException(httpStatus, this);
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CreateQuizRequest implements BaseRequest {

        private Quiz.QuizType type;
        private int quizCount;
        private String plainText;

        @Override
        public boolean isValid() {
            return type != null && plainText != null && !plainText.isBlank() && quizCount > 0;
        }
    }

    @Getter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CreateQuizResponse extends BaseResponse<CreateQuizErrorCode> {

        private List<GeneratedQuizResponse> generatedQuizResponseList;
    }
}
