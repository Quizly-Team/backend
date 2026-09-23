package org.quizly.quizly.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.external.openai.service.CreateTextOpenAiService;
import org.quizly.quizly.quiz.dto.response.QuizTagResponse;
import org.quizly.quizly.quiz.service.CreateQuizStructureService.CreateQuizStructureRequest;
import org.quizly.quizly.quiz.service.CreateQuizStructureService.CreateQuizStructureResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class CreateQuizStructureService implements
    BaseService<CreateQuizStructureRequest, CreateQuizStructureResponse> {

    private static final String QUIZ_STRUCTURE_PROMPT_PATH =
        "prompt/basic_quiz/structure.txt";

    private final CreateTextOpenAiService createTextOpenAiService;
    private final ObjectMapper objectMapper;

    @Override
    public CreateQuizStructureResponse execute(CreateQuizStructureRequest request) {

        if (request == null || !request.isValid()) {
            return CreateQuizStructureResponse.builder()
                .success(false)
                .errorCode(CreateQuizStructureErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
                .build();
        }

        CreateTextOpenAiService.CreateTextOpenAiResponse openAiResponse =
            createTextOpenAiService.execute(
                CreateTextOpenAiService.CreateTextOpenAiRequest.builder()
                    .inputText(request.getPlainText())
                    .promptPath(QUIZ_STRUCTURE_PROMPT_PATH)
                    .build()
            );

        if (!openAiResponse.isSuccess()) {
            log.warn("[CreateQuizStructureService] Failed to create quiz structure.");

            return CreateQuizStructureResponse.builder()
                .success(false)
                .errorCode(CreateQuizStructureErrorCode.FAILED_CREATE_QUIZ_STRUCTURE)
                .build();
        }

        try {
            String resultText = normalizeJsonResponse(
                openAiResponse.getResultText()
            );

            QuizStructureResult result =
                objectMapper.readValue(
                    resultText,
                    QuizStructureResult.class
                );

            if (result.getTags() == null || result.getTags().isEmpty()) {
                return CreateQuizStructureResponse.builder()
                    .success(false)
                    .errorCode(CreateQuizStructureErrorCode.EMPTY_QUIZ_STRUCTURE)
                    .build();
            }

            return CreateQuizStructureResponse.builder()
                .success(true)
                .tags(result.getTags())
                .build();

        } catch (JsonProcessingException e) {
            log.error(
                "[CreateQuizStructureService] Failed to parse quiz structure.",
                e
            );

            return CreateQuizStructureResponse.builder()
                .success(false)
                .errorCode(CreateQuizStructureErrorCode.FAILED_PARSE_QUIZ_STRUCTURE)
                .build();
        }
    }

    private String normalizeJsonResponse(String resultText) {

        if (resultText == null) {
            return "";
        }

        String normalized = resultText.trim();

        if (normalized.startsWith("```")) {
            normalized = normalized
                .replaceFirst("^```json\\s*", "")
                .replaceFirst("^```\\s*", "")
                .replaceFirst("\\s*```$", "")
                .trim();
        }

        return normalized;
    }

    @Getter
    @RequiredArgsConstructor
    public enum CreateQuizStructureErrorCode
        implements BaseErrorCode<DomainException> {

        NOT_EXIST_REQUIRED_PARAMETER(
            HttpStatus.BAD_REQUEST,
            "요청 파라미터가 존재하지 않습니다."
        ),

        FAILED_CREATE_QUIZ_STRUCTURE(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "퀴즈 태그 생성에 실패하였습니다."
        ),

        FAILED_PARSE_QUIZ_STRUCTURE(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "퀴즈 태그 응답 파싱에 실패하였습니다."
        ),

        EMPTY_QUIZ_STRUCTURE(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "생성된 퀴즈 태그가 존재하지 않습니다."
        );

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
    public static class CreateQuizStructureRequest implements BaseRequest {

        private String plainText;

        @Override
        public boolean isValid() {
            return plainText != null && !plainText.isBlank();
        }
    }

    @Getter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CreateQuizStructureResponse
        extends BaseResponse<CreateQuizStructureErrorCode> {

        private List<QuizTagResponse> tags;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class QuizStructureResult {

        private List<QuizTagResponse> tags;
    }
}