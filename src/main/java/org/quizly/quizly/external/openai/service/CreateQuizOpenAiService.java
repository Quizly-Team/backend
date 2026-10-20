package org.quizly.quizly.external.openai.service;

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
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domain.entity.Quiz;
import org.quizly.quizly.core.util.TextResourceReaderUtil;
import org.quizly.quizly.external.openai.client.OpenAiChatCompletionClient;
import org.quizly.quizly.external.openai.client.OpenAiChatCompletionClient.ChatCompletionResult;
import org.quizly.quizly.external.openai.dto.Request.OpenAiRequest.ResponseFormat;
import org.quizly.quizly.external.openai.error.OpenAiErrorCode;
import org.quizly.quizly.external.openai.service.CreateQuizOpenAiService.CreateQuizOpenAiRequest;
import org.quizly.quizly.external.openai.service.CreateQuizOpenAiService.CreateQuizOpenAiResponse;
import org.quizly.quizly.external.openai.util.OpenAiUserInputUtil;
import org.quizly.quizly.quiz.dto.response.GeneratedQuizResponse;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class CreateQuizOpenAiService implements
    BaseService<CreateQuizOpenAiRequest, CreateQuizOpenAiResponse> {

    private final OpenAiChatCompletionClient openAiChatCompletionClient;
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

        ChatCompletionResult result = openAiChatCompletionClient.complete(
            getPrompt(request.getPromptPath()),
            OpenAiUserInputUtil.createUserInput(request.getPlainText(),
                request.getAnswerList()),
            request.getResponseFormat());
        if (!result.isSuccess()) {
            return CreateQuizOpenAiResponse.builder()
                .success(false)
                .errorCode(result.errorCode())
                .build();
        }

        String content = result.content();
        try {
            JsonNode quizzesNode = objectMapper.readTree(content).path("quizzes");
            if (!quizzesNode.isArray()) {
                log.warn(
                    "[CreateQuizOpenAiService] Failed to find 'quizzes' array in content. Original content: {}",
                    content);
                return CreateQuizOpenAiResponse.builder()
                    .success(false)
                    .errorCode(OpenAiErrorCode.FAILED_PARSE_OPENAI_RESPONSE)
                    .build();
            }

            List<GeneratedQuizResponse> generatedQuizResponseList = objectMapper.convertValue(
                quizzesNode, new TypeReference<List<GeneratedQuizResponse>>() {
                });
            shuffleMultipleChoiceOptions(generatedQuizResponseList);

            return CreateQuizOpenAiResponse.builder()
                .generatedQuizResponseList(generatedQuizResponseList)
                .success(true)
                .build();
        } catch (IOException e) {
            log.error("[CreateQuizOpenAiService] Failed to parse quiz content. Content: {}",
                content, e);
            return CreateQuizOpenAiResponse.builder()
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

    private void shuffleMultipleChoiceOptions(List<GeneratedQuizResponse> responseList) {
        responseList.stream()
            .filter(quiz -> quiz.getType() == Quiz.QuizType.MULTIPLE_CHOICE)
            .filter(
                quiz -> quiz.getOptions() != null && !quiz.getOptions().contains(quiz.getAnswer()))
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
        private List<String> answerList;

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
