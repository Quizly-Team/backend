package org.quizly.quizly.chatbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.core.util.TextResourceReaderUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class CreateChatMessageService implements
    BaseService<CreateChatMessageService.ChatMessageRequest,
        CreateChatMessageService.ChatMessageResponse> {

    private static final String PROMPT_PATH = "prompt/chatbot/quiz_chatbot_system.txt";
    private static final int MAX_MESSAGES = 20;

    private final ChatClient chatClient;
    private final ChatMemoryRepository chatMemoryRepository;
    private final TextResourceReaderUtil textResourceReaderUtil;
    private final ObjectMapper objectMapper;

    @Override
    public ChatMessageResponse execute(ChatMessageRequest request) {
        if (request == null || !request.isValid()) {
            return ChatMessageResponse.builder()
                .success(false)
                .errorCode(ChatbotErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
                .build();
        }

        String internalConversationId = request.getUserId() + ":" + request.getConversationId();

        String systemPrompt = textResourceReaderUtil.load(PROMPT_PATH)
            .replace("{{quizContext}}", buildQuizContext(request));

        List<Message> history = chatMemoryRepository.findByConversationId(internalConversationId);

        try {
            ChatResponse chatResponse = chatClient.prompt()
                .system(systemPrompt)
                .messages(history)
                .user(request.getUserMessage())
                .options(OpenAiChatOptions.builder()
                    .responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build())
                    .build())
                .call()
                .chatResponse();

            if (chatResponse == null || chatResponse.getResult() == null) {
                return ChatMessageResponse.builder()
                    .success(false)
                    .errorCode(ChatbotErrorCode.CHATBOT_AI_FAILED)
                    .build();
            }

            String content = chatResponse.getResult().getOutput().getText();
            KeywordChatResponse aiResponse = objectMapper.readValue(content, KeywordChatResponse.class);

            if (aiResponse == null) {
                return ChatMessageResponse.builder()
                    .success(false)
                    .errorCode(ChatbotErrorCode.CHATBOT_AI_FAILED)
                    .build();
            }

            List<String> keywords = aiResponse.keywords() != null ? aiResponse.keywords() : List.of();

            List<Message> updated = new ArrayList<>(history);
            updated.add(new UserMessage(request.getUserMessage()));
            updated.add(new AssistantMessage(String.join(", ", keywords)));

            if (updated.size() > MAX_MESSAGES) {
                updated = new ArrayList<>(updated.subList(updated.size() - MAX_MESSAGES, updated.size()));
            }

            chatMemoryRepository.saveAll(internalConversationId, updated);

            return ChatMessageResponse.builder()
                .success(true)
                .conversationId(request.getConversationId())
                .assistantMessage(aiResponse.answer())
                .build();

        } catch (Exception e) {
            log.error("[CreateChatMessageService] AI call failed", e);
            return ChatMessageResponse.builder()
                .success(false)
                .errorCode(ChatbotErrorCode.CHATBOT_AI_FAILED)
                .build();
        }
    }

    private String buildQuizContext(ChatMessageRequest request) {
        return "문제: " + request.getQuestion() + "\n정답: " + request.getAnswer();
    }

    record KeywordChatResponse(String answer, List<String> keywords) {}

    @Getter
    @RequiredArgsConstructor
    public enum ChatbotErrorCode implements BaseErrorCode<DomainException> {

        NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "챗봇 요청 파라미터가 없습니다."),
        CHATBOT_AI_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 챗봇 응답에 실패했습니다.");

        private final HttpStatus httpStatus;
        private final String message;

        @Override
        public DomainException toException() {
            return new DomainException(httpStatus, this);
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageRequest implements BaseRequest {

        private String conversationId;
        private String question;
        private String answer;
        private String userMessage;
        private Long userId;

        @Override
        public boolean isValid() {
            return conversationId != null && !conversationId.isBlank()
                && question != null && !question.isBlank()
                && answer != null && !answer.isBlank()
                && userMessage != null && !userMessage.isBlank()
                && userId != null;
        }
    }

    @Getter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageResponse extends BaseResponse<ChatbotErrorCode> {

        private String conversationId;
        private String assistantMessage;
    }
}
