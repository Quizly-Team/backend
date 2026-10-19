package org.quizly.quizly.external.openai.client;

import static org.quizly.quizly.core.util.okhttp.OkHttpRequest.createRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.quizly.quizly.external.openai.dto.Request.OpenAiRequest;
import org.quizly.quizly.external.openai.dto.Request.OpenAiRequest.ResponseFormat;
import org.quizly.quizly.external.openai.dto.Response.OpenAiResponse;
import org.quizly.quizly.external.openai.error.OpenAiErrorCode;
import org.quizly.quizly.external.openai.property.OpenAiProperty;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class OpenAiChatCompletionClient {

    private static final MediaType JSON = MediaType.parse("application/json");

    private final OpenAiProperty openAiProperty;
    private final ObjectMapper objectMapper;

    public ChatCompletionResult complete(String systemPrompt, String userInput,
        ResponseFormat responseFormat) {
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(OpenAiRequest.of(
                openAiProperty.getModel(),
                systemPrompt,
                userInput,
                responseFormat,
                openAiProperty.getReasoningEffort()));
        } catch (JsonProcessingException e) {
            log.error("[OpenAiChatCompletionClient] Failed to serialize OpenAi request", e);
            return ChatCompletionResult.failure(OpenAiErrorCode.FAILED_CREATE_OPENAI_REQUEST);
        }

        Request httpRequest = new Request.Builder()
            .url(openAiProperty.getUrl())
            .header("Authorization", "Bearer " + openAiProperty.getKey())
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .post(RequestBody.create(requestBody, JSON))
            .build();

        try (Response response = createRequest(httpRequest)) {
            ResponseBody body = response.body();
            if (body == null) {
                log.warn("[OpenAiChatCompletionClient] response body is null. response code: {}",
                    response.code());
                return ChatCompletionResult.failure(OpenAiErrorCode.EMPTY_OPENAI_RESPONSE_BODY);
            }

            String responseBody = body.string();
            if (!response.isSuccessful()) {
                log.warn(
                    "[OpenAiChatCompletionClient] OpenAi API returned non-successful code: {}, body: {}",
                    response.code(), responseBody);
                return ChatCompletionResult.failure(OpenAiErrorCode.FAILED_CREATE_OPENAI_REQUEST);
            }

            String content;
            try {
                content = objectMapper.readValue(responseBody, OpenAiResponse.class).extractText();
            } catch (IOException e) {
                log.error("[OpenAiChatCompletionClient] Failed to read OpenAi response. Body: {}",
                    responseBody, e);
                return ChatCompletionResult.failure(OpenAiErrorCode.FAILED_READ_OPENAI_RESPONSE);
            }

            if (content == null || content.isBlank()) {
                log.warn("[OpenAiChatCompletionClient] Empty content in OpenAi response. Body: {}",
                    responseBody);
                return ChatCompletionResult.failure(OpenAiErrorCode.EMPTY_OPENAI_RESPONSE_BODY);
            }
            return ChatCompletionResult.success(content);

        } catch (IOException e) {
            log.error(
                "[OpenAiChatCompletionClient] OpenAi API request failed due to network I/O error.",
                e);
            return ChatCompletionResult.failure(OpenAiErrorCode.OPENAI_NETWORK_ERROR);
        }
    }

    public record ChatCompletionResult(String content, OpenAiErrorCode errorCode) {

        static ChatCompletionResult success(String content) {
            return new ChatCompletionResult(content, null);
        }

        static ChatCompletionResult failure(OpenAiErrorCode errorCode) {
            return new ChatCompletionResult(null, errorCode);
        }

        public boolean isSuccess() {
            return errorCode == null;
        }
    }
}
