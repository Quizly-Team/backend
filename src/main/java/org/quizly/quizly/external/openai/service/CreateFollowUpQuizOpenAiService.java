package org.quizly.quizly.external.openai.service;

import static org.quizly.quizly.core.util.okhttp.OkHttpRequest.createRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domain.entity.Quiz;
import org.quizly.quizly.core.util.TextResourceReaderUtil;
import org.quizly.quizly.core.util.okhttp.OkHttpJsonRequest;
import org.quizly.quizly.external.openai.dto.Request.OpenAiRequest;
import org.quizly.quizly.external.openai.dto.Request.OpenAiRequest.ResponseFormat;
import org.quizly.quizly.external.openai.dto.Response.OpenAiResponse;
import org.quizly.quizly.external.openai.error.OpenAiErrorCode;
import org.quizly.quizly.external.openai.property.OpenAiProperty;
import org.quizly.quizly.external.openai.service.CreateFollowUpQuizOpenAiService.CreateFollowUpQuizOpenAiRequest;
import org.quizly.quizly.external.openai.service.CreateFollowUpQuizOpenAiService.CreateFollowUpQuizOpenAiResponse;
import org.quizly.quizly.quiz.dto.response.GeneratedQuizResponse;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class CreateFollowUpQuizOpenAiService implements
    BaseService<CreateFollowUpQuizOpenAiRequest, CreateFollowUpQuizOpenAiResponse> {

    private static final double QUIZ_TEMPERATURE = 0.7;

    private static final Map<Quiz.QuizType, String> FOLLOW_UP_PROMPT_PATHS = Map.of(
        Quiz.QuizType.MULTIPLE_CHOICE, "prompt/basic_quiz/follow_up_multiple_choice.txt",
        Quiz.QuizType.TRUE_FALSE, "prompt/basic_quiz/follow_up_true_false.txt"
    );

    private final OpenAiProperty openAiProperty;
    private final ObjectMapper objectMapper;
    private final TextResourceReaderUtil textResourceReaderUtil;

    @Override
    public CreateFollowUpQuizOpenAiResponse execute(CreateFollowUpQuizOpenAiRequest request) {
        if (request == null || !request.isValid()) {
            return CreateFollowUpQuizOpenAiResponse.builder()
                .success(false)
                .errorCode(OpenAiErrorCode.NOT_EXIST_OPENAI_REQUIRED_PARAMETER)
                .build();
        }

        String promptPath = FOLLOW_UP_PROMPT_PATHS.get(request.getQuizType());
        if (promptPath == null) {
            log.warn("[CreateFollowUpQuizOpenAiService] Could not find a prompt path. QuizType: {}",
                request.getQuizType());
            return CreateFollowUpQuizOpenAiResponse.builder()
                .success(false)
                .errorCode(OpenAiErrorCode.PROMPT_FILE_NOT_FOUND)
                .build();
        }

        String systemContent = getPrompt(promptPath);
        String requestBody = createOpenAiRequestBody(request, systemContent);

        Request httpRequest = new Request.Builder()
            .url(openAiProperty.getUrl())
            .header("Authorization", "Bearer " + openAiProperty.getKey())
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
            .build();

        try (Response response = createRequest(httpRequest)) {
            ResponseBody body = response.body();
            if (body == null) {
                log.warn("[CreateFollowUpQuizOpenAiService] response body is null. response code: {}",
                    response.code());
                return CreateFollowUpQuizOpenAiResponse.builder()
                    .success(false)
                    .errorCode(OpenAiErrorCode.EMPTY_OPENAI_RESPONSE_BODY)
                    .build();
            }

            String responseBody = body.string();

            if (!response.isSuccessful()) {
                log.warn(
                    "[CreateFollowUpQuizOpenAiService] OpenAi API returned non-successful code: {}, body: {}",
                    response.code(), responseBody);
                return CreateFollowUpQuizOpenAiResponse.builder()
                    .success(false)
                    .errorCode(OpenAiErrorCode.FAILED_CREATE_OPENAI_REQUEST)
                    .build();
            }

            try {
                OpenAiResponse openAiResponse = objectMapper.readValue(responseBody,
                    OpenAiResponse.class);
                String content = openAiResponse.extractText();

                if (content == null || content.isBlank()) {
                    log.warn(
                        "[CreateFollowUpQuizOpenAiService] Empty content in OpenAi response. Body: {}",
                        responseBody);
                    return CreateFollowUpQuizOpenAiResponse.builder()
                        .success(false)
                        .errorCode(OpenAiErrorCode.EMPTY_OPENAI_RESPONSE_BODY)
                        .build();
                }

                JsonNode rootNode = objectMapper.readTree(content);
                JsonNode quizzesNode = rootNode.path("quizzes");

                if (!quizzesNode.isArray() || quizzesNode.isEmpty()) {
                    log.warn(
                        "[CreateFollowUpQuizOpenAiService] Failed to find 'quizzes' array in content. Original content: {}",
                        content);
                    return CreateFollowUpQuizOpenAiResponse.builder()
                        .success(false)
                        .errorCode(OpenAiErrorCode.FAILED_PARSE_OPENAI_RESPONSE)
                        .build();
                }

                List<GeneratedQuizResponse> generatedQuizResponseList = objectMapper.convertValue(
                    quizzesNode, new TypeReference<List<GeneratedQuizResponse>>() {
                    });

                GeneratedQuizResponse generatedQuiz = generatedQuizResponseList.get(0);
                generatedQuiz.setSourceText(request.getSourceText());
                if (generatedQuiz.getType() == Quiz.QuizType.MULTIPLE_CHOICE) {
                    shuffleMultipleChoiceOptions(generatedQuiz);
                }

                return CreateFollowUpQuizOpenAiResponse.builder()
                    .generatedQuiz(generatedQuiz)
                    .success(true)
                    .build();

            } catch (IOException e) {
                log.error(
                    "[CreateFollowUpQuizOpenAiService] Failed to parse OpenAi API response. Body: {}",
                    responseBody, e);
                return CreateFollowUpQuizOpenAiResponse.builder()
                    .success(false)
                    .errorCode(OpenAiErrorCode.FAILED_READ_OPENAI_RESPONSE)
                    .build();
            }

        } catch (IOException e) {
            log.error(
                "[CreateFollowUpQuizOpenAiService] OpenAi API request failed due to network I/O error.",
                e);
            return CreateFollowUpQuizOpenAiResponse.builder()
                .success(false)
                .errorCode(OpenAiErrorCode.OPENAI_NETWORK_ERROR)
                .build();
        }
    }

    private String createOpenAiRequestBody(CreateFollowUpQuizOpenAiRequest request,
        String systemContent) {
        OpenAiRequest openAiRequest = OpenAiRequest.from(
            systemContent,
            createUserMessage(request.getSourceText(), request.getPreviousQuizzes()),
            openAiProperty.getModel(),
            QUIZ_TEMPERATURE,
            request.getResponseFormat()
        );

        String jsonBody = new OkHttpJsonRequest(openAiRequest).convertRequestToString();
        log.debug("Request JSON Body: {}", jsonBody);
        return jsonBody;
    }

    private String createUserMessage(String sourceText,
        List<PreviousQuizContext> previousQuizzes) {
        PreviousQuizContext currentQuiz = previousQuizzes.get(previousQuizzes.size() - 1);
        List<PreviousQuizContext> historyQuizzes = previousQuizzes.subList(0,
            previousQuizzes.size() - 1);

        StringBuilder sb = new StringBuilder();
        sb.append("<참고 텍스트> ").append(sourceText).append("\n\n");
        sb.append("<현재 선택된 문제(부모 문제) - 새 문제는 반드시 이 문제와 같은 개념/주제를 다룰 것>\n");
        sb.append("문제: ").append(currentQuiz.quizText()).append('\n');
        sb.append("보기: ").append(String.join(" / ", currentQuiz.options())).append('\n');
        sb.append("정답: ").append(currentQuiz.answer()).append('\n');
        sb.append("해설: ").append(currentQuiz.explanation()).append("\n\n");

        if (historyQuizzes.isEmpty()) {
            sb.append("<지금까지 출제된 문제 목록> 없음 (현재 선택된 문제가 이 개념의 첫 문제)\n");
        } else {
            sb.append(
                "<지금까지 출제된 문제 목록 (문제만 - 참고용) - 아래 문제들과 중복/유사한 문제를 출제하지 말 것>\n");
            for (int i = 0; i < historyQuizzes.size(); i++) {
                PreviousQuizContext previous = historyQuizzes.get(i);
                sb.append(i + 1).append(". 문제: ").append(previous.quizText()).append('\n');
            }
        }
        return sb.toString();
    }

    private String getPrompt(String promptPath) {
        String prompt = textResourceReaderUtil.load(promptPath);
        if (prompt == null || prompt.isBlank()) {
            throw OpenAiErrorCode.PROMPT_FILE_NOT_FOUND.toException();
        }
        return prompt;
    }

    private void shuffleMultipleChoiceOptions(GeneratedQuizResponse quiz) {
        if (quiz.getOptions() == null || quiz.getOptions().contains(quiz.getAnswer())) {
            return;
        }
        if (quiz.getOptions().isEmpty()) {
            quiz.getOptions().add(quiz.getAnswer());
        } else {
            quiz.getOptions().set(0, quiz.getAnswer());
        }
    }

    public record PreviousQuizContext(String quizText, String answer, List<String> options,
        String explanation) {

        public static PreviousQuizContext questionOnly(String quizText) {
            return new PreviousQuizContext(quizText, null, null, null);
        }


    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CreateFollowUpQuizOpenAiRequest implements BaseRequest {

        private String sourceText;
        private List<PreviousQuizContext> previousQuizzes;
        private Quiz.QuizType quizType;
        private ResponseFormat responseFormat;

        @Override
        public boolean isValid() {
            return sourceText != null && !sourceText.isBlank()
                && previousQuizzes != null && !previousQuizzes.isEmpty()
                && quizType != null && responseFormat != null;
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CreateFollowUpQuizOpenAiResponse extends BaseResponse<OpenAiErrorCode> {

        private GeneratedQuizResponse generatedQuiz;
    }
}
