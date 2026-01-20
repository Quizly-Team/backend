package org.quizly.quizly.external.openai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.util.TextResourceReaderUtil;
import org.quizly.quizly.external.openai.dto.Request.OpenAiRequest;
import org.quizly.quizly.external.openai.dto.Response.OpenAiResponse;
import org.quizly.quizly.external.openai.error.OpenAiErrorCode;
import org.quizly.quizly.external.openai.property.OpenAiProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static org.quizly.quizly.core.util.okhttp.OkHttpRequest.createRequest;

@Log4j2
@Service
@RequiredArgsConstructor
public class CreateTextOpenAiService implements BaseService<CreateTextOpenAiService.CreateTextOpenAiRequest, CreateTextOpenAiService.CreateTextOpenAiResponse> {

    private final OpenAiProperty openAiProperty;
    private final ObjectMapper objectMapper;
    private final TextResourceReaderUtil textResourceReaderUtil;

    @Override
    public CreateTextOpenAiResponse execute(CreateTextOpenAiRequest request) {
       if (request == null || !request.isValid()){
           return CreateTextOpenAiResponse.builder()
               .success(false)
               .errorCode(OpenAiErrorCode.NOT_EXIST_OPENAI_REQUIRED_PARAMETER)
               .build();
       }

       String systemPrompt = getPrompt(request.getPromptPath());
        String requestBody;

        try {
            String systemPromptWithData = systemPrompt.replace("{{analysisTargetText}}", request.getInputText());
            OpenAiRequest openAiRequest =
                OpenAiRequest.from(
                    systemPromptWithData,
                    "",
                    openAiProperty.getModel(),
                    0.7
                );

            requestBody = objectMapper.writeValueAsString(openAiRequest);
        } catch (JsonProcessingException e) {
            log.error("[CreateTextOpenAiService] Failed to serialize OpenAi request", e);
            return CreateTextOpenAiService.CreateTextOpenAiResponse.builder()
                .success(false)
                .errorCode(OpenAiErrorCode.FAILED_CREATE_OPENAI_REQUEST)
                .build();
        }

        String url = openAiProperty.getUrl();

        Request httpRequest = new Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + openAiProperty.getKey())
            .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
            .build();


        try (Response response = createRequest(httpRequest)) {
            if (response.body() == null) {
                return CreateTextOpenAiService.CreateTextOpenAiResponse.builder()
                    .success(false)
                    .errorCode(OpenAiErrorCode.EMPTY_OPENAI_RESPONSE_BODY)
                    .build();
            }

            String responseBody = response.body().string();

            if (!response.isSuccessful()) {
                log.error("[CreateTextOpenAiService] OpenAi error {} body={}", response.code(), responseBody);
                return CreateTextOpenAiService.CreateTextOpenAiResponse.builder()
                    .success(false)
                    .errorCode(OpenAiErrorCode.FAILED_CREATE_OPENAI_REQUEST)
                    .build();
            }

            OpenAiResponse openAiResponse =
                objectMapper.readValue(responseBody, OpenAiResponse.class);

            String content = openAiResponse.extractText();

            if (content == null || content.isBlank()) {
                return CreateTextOpenAiService.CreateTextOpenAiResponse.builder()
                    .success(false)
                    .errorCode(OpenAiErrorCode.EMPTY_OPENAI_RESPONSE_BODY)
                    .build();
            }

            return CreateTextOpenAiService.CreateTextOpenAiResponse.builder()
                .resultText(content)
                .success(true)
                .build();


        } catch (IOException e) {
            log.error("[CreateTextOpenAiService] OpenAi I/O error", e);
            return CreateTextOpenAiService.CreateTextOpenAiResponse.builder()
                .success(false)
                .errorCode(OpenAiErrorCode.OPENAI_NETWORK_ERROR)
                .build();
        }
    }

    private String getPrompt(String path) {
        String prompt = textResourceReaderUtil.load(path);
        if (prompt == null || prompt.isBlank()) {
            throw OpenAiErrorCode.PROMPT_FILE_NOT_FOUND.toException();
        }
        return prompt;
    }
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateTextOpenAiRequest implements BaseRequest{
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
    public static class CreateTextOpenAiResponse extends BaseResponse<OpenAiErrorCode> {
        private String resultText;
    }
}
