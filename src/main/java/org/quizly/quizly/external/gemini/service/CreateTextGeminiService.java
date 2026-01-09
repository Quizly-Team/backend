package org.quizly.quizly.external.gemini.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import okhttp3.*;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.util.TextResourceReaderUtil;
import org.quizly.quizly.external.gemini.dto.Request.GeminiRequest;
import org.quizly.quizly.external.gemini.dto.Response.GeminiResponse;
import org.quizly.quizly.external.gemini.error.GeminiErrorCode;
import org.quizly.quizly.external.gemini.property.GeminiProperty;
import org.springframework.stereotype.Service;
import static org.quizly.quizly.core.util.okhttp.OkHttpRequest.createRequest;

@Log4j2
@Service
@RequiredArgsConstructor
public class CreateTextGeminiService implements
        BaseService<CreateTextGeminiService.CreateTextGeminiRequest,
            CreateTextGeminiService.CreateTextGeminiResponse> {

    private final GeminiProperty geminiProperty;
    private final ObjectMapper objectMapper;
    private final TextResourceReaderUtil textResourceReaderUtil;

    @Override
    public CreateTextGeminiResponse execute(CreateTextGeminiRequest request) {
        if (request == null || !request.isValid()) {
            return CreateTextGeminiResponse.builder()
                    .success(false)
                    .errorCode(GeminiErrorCode.NOT_EXIST_GEMINI_REQUIRED_PARAMETER)
                    .build();
        }

        String systemContent = getPrompt(request.getPromptPath());
        String fullPrompt = systemContent.replace(
            "{{analysisTargetText}}",
            request.getInputText()
        );
        String requestBody;
        try {
            GeminiRequest geminiRequest = GeminiRequest.from(fullPrompt);
            requestBody = objectMapper.writeValueAsString(geminiRequest);
        } catch (JsonProcessingException e) {
            log.error("[CreateTextGeminiService] Failed to serialize Gemini request", e);
            return CreateTextGeminiResponse.builder()
                .success(false)
                .errorCode(GeminiErrorCode.FAILED_GEMINI_REQUEST)
                .build();
        }

        String url = "%s/models/%s:generateContent?key=%s"
                .formatted(
                        geminiProperty.getUrl().replaceAll("/$", ""),
                        geminiProperty.getModel(),
                        geminiProperty.getKey()
                );

        Request httpRequest = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();

        try (Response response = createRequest(httpRequest)) {
            if (response.body() == null) {
                return CreateTextGeminiResponse.builder()
                        .success(false)
                        .errorCode(GeminiErrorCode.EMPTY_GEMINI_RESPONSE_BODY)
                        .build();
            }

            String responseBody = response.body().string();

            if (!response.isSuccessful()) {
                log.error("[CreateTextGeminiService] Gemini error {} body={}", response.code(), responseBody);
                return CreateTextGeminiResponse.builder()
                        .success(false)
                        .errorCode(GeminiErrorCode.FAILED_GEMINI_REQUEST)
                        .build();
            }

            GeminiResponse geminiResponse =
                objectMapper.readValue(responseBody, GeminiResponse.class);

            String content = geminiResponse.extractText();

            if (content == null || content.isBlank()) {
                return CreateTextGeminiResponse.builder()
                    .success(false)
                    .errorCode(GeminiErrorCode.EMPTY_GEMINI_RESPONSE_BODY)
                    .build();
            }

            return CreateTextGeminiResponse.builder()
                    .resultText(content)
                    .success(true)
                    .build();

        } catch (IOException e) {
            log.error("[CreateTextGeminiService] Gemini I/O error", e);
            return CreateTextGeminiResponse.builder()
                    .success(false)
                    .errorCode(GeminiErrorCode.GEMINI_NETWORK_ERROR)
                    .build();
        }
    }

    private String getPrompt(String path) {
        String prompt = textResourceReaderUtil.load(path);
        if (prompt == null || prompt.isBlank()) {
            throw GeminiErrorCode.PROMPT_FILE_NOT_FOUND.toException();
        }
        return prompt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateTextGeminiRequest implements BaseRequest {
        private String inputText;
        private String promptPath;

        @Override
        public boolean isValid() {
            return inputText != null && !inputText.isBlank()
                    && promptPath != null && !promptPath.isBlank();
        }
    }

    @Getter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateTextGeminiResponse
            extends BaseResponse<GeminiErrorCode> {

        private String resultText;
    }
}
