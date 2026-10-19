package org.quizly.quizly.external.openai.service;

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
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.util.TextResourceReaderUtil;
import org.quizly.quizly.external.openai.client.OpenAiChatCompletionClient;
import org.quizly.quizly.external.openai.client.OpenAiChatCompletionClient.ChatCompletionResult;
import org.quizly.quizly.external.openai.error.OpenAiErrorCode;
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

    private final OpenAiChatCompletionClient openAiChatCompletionClient;
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

        ChatCompletionResult result = openAiChatCompletionClient.complete(
            getPrompt(request.getPromptPath()),
            "<입력으로 들어온 정리> " + request.getPlainText(),
            OpenAiResponseFormatUtil.createTopicResponseFormat());
        if (!result.isSuccess()) {
            return CreateTopicOpenAiResponse.builder()
                .success(false)
                .errorCode(result.errorCode())
                .build();
        }

        String content = result.content();
        try {
            GeneratedTopicResponse generatedTopicResponse = objectMapper.readValue(content,
                GeneratedTopicResponse.class);
            if (generatedTopicResponse == null || generatedTopicResponse.getTopic() == null) {
                log.warn("[CreateTopicOpenAiService] Failed to parse topic from content: {}",
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
            log.error("[CreateTopicOpenAiService] Failed to parse topic content. Content: {}",
                content, e);
            return CreateTopicOpenAiResponse.builder()
                .success(false)
                .errorCode(OpenAiErrorCode.FAILED_READ_OPENAI_RESPONSE)
                .build();
        }
    }

    private String getPrompt(String promptPath) {
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
