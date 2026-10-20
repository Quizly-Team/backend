package org.quizly.quizly.external.openai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
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
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.util.TextResourceReaderUtil;
import org.quizly.quizly.external.openai.client.OpenAiChatCompletionClient;
import org.quizly.quizly.external.openai.client.OpenAiChatCompletionClient.ChatCompletionResult;
import org.quizly.quizly.external.openai.dto.Request.OpenAiRequest.ResponseFormat;
import org.quizly.quizly.external.openai.error.OpenAiErrorCode;
import org.quizly.quizly.external.openai.service.CreateMockExamOpenAiService.CreateMockExamOpenAiRequest;
import org.quizly.quizly.external.openai.service.CreateMockExamOpenAiService.CreateMockExamOpenAiResponse;
import org.quizly.quizly.external.openai.util.OpenAiUserInputUtil;
import org.quizly.quizly.mock.dto.response.GeneratedMockExamResponse;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class CreateMockExamOpenAiService implements
    BaseService<CreateMockExamOpenAiRequest, CreateMockExamOpenAiResponse> {

    private final OpenAiChatCompletionClient openAiChatCompletionClient;
    private final ObjectMapper objectMapper;
    private final TextResourceReaderUtil textResourceReaderUtil;

    @Override
    public CreateMockExamOpenAiResponse execute(CreateMockExamOpenAiRequest request) {
        if (request == null || !request.isValid()) {
            return CreateMockExamOpenAiResponse.builder()
                .success(false)
                .errorCode(OpenAiErrorCode.NOT_EXIST_OPENAI_REQUIRED_PARAMETER)
                .build();
        }

        ChatCompletionResult result = openAiChatCompletionClient.complete(
            getPrompt(request.getPromptPath()),
            OpenAiUserInputUtil.createUserInput(request.getPlainText(),
                request.getAnswerList()),
            request.getResponseFormat());
        if (!result.isSuccess()) {
            return CreateMockExamOpenAiResponse.builder()
                .success(false)
                .errorCode(result.errorCode())
                .build();
        }

        String content = result.content();
        try {
            JsonNode quizzesNode = objectMapper.readTree(content).path("quizzes");
            if (!quizzesNode.isArray()) {
                log.warn(
                    "[CreateMockExamOpenAiService] Failed to find 'quizzes' array in content. Original content: {}",
                    content);
                return CreateMockExamOpenAiResponse.builder()
                    .success(false)
                    .errorCode(OpenAiErrorCode.FAILED_PARSE_OPENAI_RESPONSE)
                    .build();
            }

            List<GeneratedMockExamResponse> generatedMockExamResponseList =
                objectMapper.convertValue(quizzesNode,
                    new TypeReference<List<GeneratedMockExamResponse>>() {
                    });

            return CreateMockExamOpenAiResponse.builder()
                .generatedMockExamResponseList(generatedMockExamResponseList)
                .success(true)
                .build();
        } catch (IOException e) {
            log.error("[CreateMockExamOpenAiService] Failed to parse mock exam content. Content: {}",
                content, e);
            return CreateMockExamOpenAiResponse.builder()
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
    public static class CreateMockExamOpenAiRequest implements BaseRequest {

        private String plainText;
        private String promptPath;
        private ResponseFormat responseFormat;
        private List<String> answerList;

        @Override
        public boolean isValid() {
            return plainText != null && !plainText.isEmpty()
                && promptPath != null && !promptPath.isEmpty()
                && responseFormat != null;
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CreateMockExamOpenAiResponse extends BaseResponse<OpenAiErrorCode> {

        private List<GeneratedMockExamResponse> generatedMockExamResponseList;
    }
}
