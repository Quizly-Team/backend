package org.quizly.quizly.external.openai.service;

import static org.quizly.quizly.core.util.okhttp.OkHttpRequest.createRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
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
import org.quizly.quizly.core.util.TextResourceReaderUtil;
import org.quizly.quizly.core.util.okhttp.OkHttpJsonRequest;
import org.quizly.quizly.external.openai.dto.Request.OpenAiRequest;
import org.quizly.quizly.external.openai.dto.Response.OpenAiResponse;
import org.quizly.quizly.external.openai.error.OpenAiErrorCode;
import org.quizly.quizly.external.openai.property.OpenAiProperty;
import org.quizly.quizly.external.openai.service.CreateTopicOpenAiService.CreateTopicOpenAiRequest;
import org.quizly.quizly.external.openai.service.CreateTopicOpenAiService.CreateTopicOpenAiResponse;
import org.quizly.quizly.external.openai.util.OpenAiResponseFormatUtil;
import org.quizly.quizly.quiz.dto.response.GeneratedTopicResponse;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class CreateTopicOpenAiService implements
    BaseService<CreateTopicOpenAiRequest, CreateTopicOpenAiResponse> {

    private static final double TOPIC_TEMPERATURE = 0.2;

    private final OpenAiProperty openAiProperty;
    private final ObjectMapper objectMapper;
    private final TextResourceReaderUtil textResourceReaderUtil;

    @Override
    public CreateTopicOpenAiResponse execute(CreateTopicOpenAiRequest request) {
        if (request == null || !request.isValid()) {
            return CreateTopicOpenAiResponse.builder()
                .success(false)
                .errorCode(OpenAiErrorCode.NOT_EXIST_OPENAI_REQUIRED_PARAMETER)
                .build();
        }

        String systemContent = getPrompt(request.getPromptPath());
        String requestBody = createOpenAiRequestBody(request.getPlainText(), systemContent);

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
                log.error("[CreateTopicOpenAiService] response body is null. response code: {}",
                    response.code());
                return CreateTopicOpenAiResponse.builder()
                    .success(false)
                    .errorCode(OpenAiErrorCode.EMPTY_OPENAI_RESPONSE_BODY)
                    .build();
            }

            String responseBody = body.string();

            if (!response.isSuccessful()) {
                log.error(
                    "[CreateTopicOpenAiService] OpenAi API returned non-successful code: {}, body: {}",
                    response.code(), responseBody);
                return CreateTopicOpenAiResponse.builder()
                    .success(false)
                    .errorCode(OpenAiErrorCode.FAILED_CREATE_OPENAI_REQUEST)
                    .build();
            }

            try {
                OpenAiResponse openAiResponse = objectMapper.readValue(responseBody,
                    OpenAiResponse.class);
                String content = openAiResponse.extractText();

                if (content == null || content.isBlank()) {
                    log.error(
                        "[CreateTopicOpenAiService] Empty content in OpenAi response. Body: {}",
                        responseBody);
                    return CreateTopicOpenAiResponse.builder()
                        .success(false)
                        .errorCode(OpenAiErrorCode.EMPTY_OPENAI_RESPONSE_BODY)
                        .build();
                }

                GeneratedTopicResponse generatedTopicResponse = objectMapper.readValue(content,
                    GeneratedTopicResponse.class);

                if (generatedTopicResponse == null || generatedTopicResponse.getTopic() == null) {
                    log.error("[CreateTopicOpenAiService] Failed to parse topic from content: {}",
                        content);
                    return CreateTopicOpenAiResponse.builder()
                        .success(false)
                        .errorCode(OpenAiErrorCode.FAILED_PARSE_OPENAI_RESPONSE)
                        .build();
                }

                return CreateTopicOpenAiResponse.builder()
                    .topic(generatedTopicResponse.getTopic())
                    .success(true)
                    .build();

            } catch (IOException e) {
                log.error(
                    "[CreateTopicOpenAiService] Failed to parse OpenAi API response. Body: {}",
                    responseBody, e);
                return CreateTopicOpenAiResponse.builder()
                    .success(false)
                    .errorCode(OpenAiErrorCode.FAILED_READ_OPENAI_RESPONSE)
                    .build();
            }

        } catch (IOException e) {
            log.error(
                "[CreateTopicOpenAiService] OpenAi API request failed due to network I/O error.",
                e);
            return CreateTopicOpenAiResponse.builder()
                .success(false)
                .errorCode(OpenAiErrorCode.OPENAI_NETWORK_ERROR)
                .build();
        }
    }

    private String createOpenAiRequestBody(String plainText, String systemContent) {
        OpenAiRequest openAiRequest = OpenAiRequest.from(
            systemContent,
            "<입력으로 들어온 정리> " + plainText,
            openAiProperty.getModel(),
            TOPIC_TEMPERATURE,
            OpenAiResponseFormatUtil.createTopicResponseFormat()
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

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CreateTopicOpenAiRequest implements BaseRequest {

        private String plainText;
        private String promptPath;

        @Override
        public boolean isValid() {
            return plainText != null && !plainText.isBlank() && promptPath != null
                && !promptPath.isBlank();
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CreateTopicOpenAiResponse extends BaseResponse<OpenAiErrorCode> {

        private String topic;
    }
}
