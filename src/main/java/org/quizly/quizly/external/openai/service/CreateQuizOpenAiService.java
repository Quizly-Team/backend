package org.quizly.quizly.external.openai.service;

import static org.quizly.quizly.core.util.okhttp.OkHttpRequest.createRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
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
import org.quizly.quizly.core.domin.entity.Quiz;
import org.quizly.quizly.core.util.TextResourceReaderUtil;
import org.quizly.quizly.core.util.okhttp.OkHttpJsonRequest;
import org.quizly.quizly.external.openai.dto.Request.OpenAiRequest;
import org.quizly.quizly.external.openai.dto.Request.OpenAiRequest.ResponseFormat;
import org.quizly.quizly.external.openai.dto.Response.OpenAiResponse;
import org.quizly.quizly.quiz.dto.response.GeneratedQuizResponse;
import org.quizly.quizly.external.openai.error.OpenAiErrorCode;
import org.quizly.quizly.external.openai.property.OpenAiProperty;
import org.quizly.quizly.external.openai.service.CreateQuizOpenAiService.CreateQuizOpenAiRequest;
import org.quizly.quizly.external.openai.service.CreateQuizOpenAiService.CreateQuizOpenAiResponse;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class CreateQuizOpenAiService implements BaseService<CreateQuizOpenAiRequest, CreateQuizOpenAiResponse> {

    private static final double QUIZ_TEMPERATURE = 0.7;

    private final OpenAiProperty openAiProperty;
    private final ObjectMapper objectMapper;
    private final TextResourceReaderUtil textResourceReaderUtil;

    @Override
    public CreateQuizOpenAiResponse execute(CreateQuizOpenAiRequest request) {
        if (request == null || !request.isValid()) {
            return CreateQuizOpenAiResponse.builder()
                .success(false)
                .errorCode(OpenAiErrorCode.NOT_EXIST_OPENAI_REQUIRED_PARAMETER)
                .build();
        }
        String systemContent = getPrompt(request.getPromptPath());
        String requestBody = createOpenAiRequestBody(request.getPlainText(), systemContent, request.getResponseFormat());

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
                log.error("[CreateQuizOpenAiService] response body is null. response code: {}", response.code());
                return CreateQuizOpenAiResponse.builder()
                    .success(false)
                    .errorCode(OpenAiErrorCode.EMPTY_OPENAI_RESPONSE_BODY)
                    .build();
            }

            String responseBody = body.string();

            if (!response.isSuccessful()) {
                log.error("[CreateQuizOpenAiService] OpenAi API returned non-successful code: {}, body: {}", response.code(), responseBody);
                return CreateQuizOpenAiResponse.builder()
                    .success(false)
                    .errorCode(OpenAiErrorCode.FAILED_CREATE_OPENAI_REQUEST)
                    .build();
            }

            try {
                OpenAiResponse openAiResponse = objectMapper.readValue(responseBody, OpenAiResponse.class);
                String content = openAiResponse.extractText();

                if (content == null || content.isBlank()) {
                    log.error("[CreateQuizOpenAiService] Empty content in OpenAi response. Body: {}", responseBody);
                    return CreateQuizOpenAiResponse.builder()
                        .success(false)
                        .errorCode(OpenAiErrorCode.EMPTY_OPENAI_RESPONSE_BODY)
                        .build();
                }

                JsonNode rootNode = objectMapper.readTree(content);
                JsonNode quizzesNode = rootNode.path("quizzes");

                if (!quizzesNode.isArray()) {
                    log.error("[CreateQuizOpenAiService] Failed to find 'quizzes' array in content. Original content: {}", content);
                    return CreateQuizOpenAiResponse.builder()
                        .success(false)
                        .errorCode(OpenAiErrorCode.FAILED_PARSE_OPENAI_RESPONSE)
                        .build();
                }

                List<GeneratedQuizResponse> generatedQuizResponseList = objectMapper.convertValue(
                    quizzesNode, new TypeReference<List<GeneratedQuizResponse>>() {});

                shuffleMultipleChoiceOptions(generatedQuizResponseList);

                return CreateQuizOpenAiResponse.builder()
                    .generatedQuizResponseList(generatedQuizResponseList)
                    .success(true)
                    .build();

            } catch (IOException e) {
                log.error("[CreateQuizOpenAiService] Failed to parse OpenAi API response. Body: {}", responseBody, e);
                return CreateQuizOpenAiResponse.builder()
                    .success(false)
                    .errorCode(OpenAiErrorCode.FAILED_READ_OPENAI_RESPONSE)
                    .build();
            }

        } catch (IOException e) {
            log.error("[CreateQuizOpenAiService] OpenAi API request failed due to network I/O error.", e);
            return CreateQuizOpenAiResponse.builder()
                .success(false)
                .errorCode(OpenAiErrorCode.OPENAI_NETWORK_ERROR)
                .build();
        }
    }

    private String createOpenAiRequestBody(String plainText, String systemContent, ResponseFormat responseFormat) {
        OpenAiRequest openAiRequest = OpenAiRequest.from(
            systemContent,
            "<입력으로 들어온 정리> " + plainText,
            openAiProperty.getModel(),
            QUIZ_TEMPERATURE,
            responseFormat
        );

        String jsonBody = new OkHttpJsonRequest(openAiRequest).convertRequestToString();
        log.debug("Request JSON Body: {}", jsonBody);
        return jsonBody;
    }

    public String getPrompt(String promptPath) {
        String prompt = textResourceReaderUtil.load(promptPath);
        if (prompt == null || prompt.isBlank()) {
            throw OpenAiErrorCode.PROMPT_FILE_NOT_FOUND.toException();
        }
        return prompt;
    }

    private void shuffleMultipleChoiceOptions(List<GeneratedQuizResponse> responseList) {
        responseList.stream()
            .filter(quiz -> quiz.getType() == Quiz.QuizType.MULTIPLE_CHOICE)
            .filter(quiz -> quiz.getOptions() != null && !quiz.getOptions().contains(quiz.getAnswer()))
            .forEach(quiz -> {
                if (quiz.getOptions().isEmpty()) {
                    quiz.getOptions().add(quiz.getAnswer());
                } else {
                    quiz.getOptions().set(0, quiz.getAnswer());
                }
            });

        responseList.stream()
            .filter(quiz -> quiz.getType() == Quiz.QuizType.MULTIPLE_CHOICE)
            .filter(quiz -> quiz.getOptions() != null && !quiz.getOptions().isEmpty())
            .forEach(quiz -> Collections.shuffle(quiz.getOptions()));
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CreateQuizOpenAiRequest implements BaseRequest {
        private String plainText;
        private String promptPath;
        private ResponseFormat responseFormat;

        @Override
        public boolean isValid() {
            return plainText != null && !plainText.isBlank()
                && promptPath != null && !promptPath.isBlank()
                && responseFormat != null;
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CreateQuizOpenAiResponse extends BaseResponse<OpenAiErrorCode> {
        private List<GeneratedQuizResponse> generatedQuizResponseList;
    }
}
