package org.quizly.quizly.external.openai.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.util.TextResourceReaderUtil;
import org.quizly.quizly.external.openai.client.OpenAiChatCompletionClient;
import org.quizly.quizly.external.openai.client.OpenAiChatCompletionClient.ChatCompletionResult;
import org.quizly.quizly.external.openai.error.OpenAiErrorCode;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateTextOpenAiService implements
    BaseService<CreateTextOpenAiService.CreateTextOpenAiRequest, CreateTextOpenAiService.CreateTextOpenAiResponse> {

    private final OpenAiChatCompletionClient openAiChatCompletionClient;
    private final TextResourceReaderUtil textResourceReaderUtil;

    @Override
    public CreateTextOpenAiResponse execute(CreateTextOpenAiRequest request) {
        if (request == null || !request.isValid()) {
            return CreateTextOpenAiResponse.builder()
                .success(false)
                .errorCode(OpenAiErrorCode.NOT_EXIST_OPENAI_REQUIRED_PARAMETER)
                .build();
        }

        String systemPromptWithData = getPrompt(request.getPromptPath())
            .replace("{{analysisTargetText}}", request.getInputText());

        ChatCompletionResult result =
            openAiChatCompletionClient.complete(systemPromptWithData, "", null);
        if (!result.isSuccess()) {
            return CreateTextOpenAiResponse.builder()
                .success(false)
                .errorCode(result.errorCode())
                .build();
        }

        return CreateTextOpenAiResponse.builder()
            .resultText(result.content())
            .success(true)
            .build();
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
    public static class CreateTextOpenAiRequest implements BaseRequest {

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
