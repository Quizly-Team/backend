package org.quizly.quizly.chatbot.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.core.util.SsePublisher;
import org.quizly.quizly.core.util.TextResourceReaderUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Log4j2
@Service
@RequiredArgsConstructor
public class CreateChatMessageService {

    private static final String PROMPT_PATH = "prompt/chatbot/quiz_chatbot_system.txt";
    private static final int MAX_MESSAGES = 20;

    private final ChatClient chatClient;
    private final ChatMemoryRepository chatMemoryRepository;
    private final TextResourceReaderUtil textResourceReaderUtil;
    private final SsePublisher ssePublisher;

    public SseEmitter execute(ChatMessageRequest request){
        if(request == null || !request.isValid()){
            SseEmitter errorEmitter = new SseEmitter();
            errorEmitter.completeWithError(ChatbotErrorCode.NOT_EXIST_REQUIRED_PARAMETER.toException());
            return errorEmitter;
        }

        SseEmitter emitter = new SseEmitter(180 * 1000L);
        String internalConversationId = request.getUserId() + ":" + request.getConversationId();

        String systemPrompt = textResourceReaderUtil.load(PROMPT_PATH)
            .replace("{{quizContext}}", buildQuizContext(request));

        List<Message> history = chatMemoryRepository.findByConversationId(internalConversationId);

        StringBuilder fullAnswer = new StringBuilder();
        AtomicBoolean isKeywordSection = new AtomicBoolean(false);

        StringBuilder buffer = new StringBuilder();
        String target = "|KEYWORDS|";

        var disposable = chatClient.prompt()
            .system(systemPrompt)
            .messages(history)
            .user(request.getUserMessage())
            .options(OpenAiChatOptions.builder()
                .build())
            .stream()
            .content()
            .subscribe(
                chunk -> {
                    fullAnswer.append(chunk);

                    if(isKeywordSection.get()) return;

                    buffer.append(chunk);
                    String currentBuffer = buffer.toString();

                    if (currentBuffer.contains(target)) {
                        isKeywordSection.set(true);
                        String validPart = currentBuffer.split(Pattern.quote(target))[0];
                        ssePublisher.sendChunk(emitter, validPart);
                        buffer.setLength(0);
                        return;
                    }

                    int overlapIndex = 0;
                    int maxOverlap = Math.min(currentBuffer.length(),target.length());

                    for(int i = maxOverlap; i>0; i--){
                        if(currentBuffer.endsWith(target.substring(0,i))){
                            overlapIndex = i;
                            break;
                        }
                    }

                    if(overlapIndex > 0){
                        String toSend = currentBuffer.substring(0, currentBuffer.length()-overlapIndex);
                        if(!toSend.isEmpty()){
                            ssePublisher.sendChunk(emitter, toSend);
                        }
                        buffer.setLength(0);
                        buffer.append(target.substring(0, overlapIndex));
                    }else{
                        ssePublisher.sendChunk(emitter,currentBuffer);
                        buffer.setLength(0);
                    }
                },
                error -> {
                    if(buffer.length()>0 && !isKeywordSection.get()){
                        ssePublisher.sendChunk(emitter,buffer.toString());
                    }
                    log.error("[CreateChatMessageService] Stream error", error);
                    emitter.completeWithError(ChatbotErrorCode.CHATBOT_AI_FAILED.toException());
                },
                () -> {
                    try {
                        if (buffer.length() > 0 && !isKeywordSection.get()) {
                            ssePublisher.sendChunk(emitter, buffer.toString());
                        }
                        String totalContent = fullAnswer.toString();
                        String messageForHistory;

                        if(totalContent.contains(target)){
                            String[] parts = totalContent.split(Pattern.quote(target));
                            messageForHistory = (parts.length > 1) ? parts[1].trim() : totalContent;
                        }else{
                            messageForHistory = totalContent;
                        }

                        saveHistory(internalConversationId,history,request.getUserMessage(),messageForHistory);

                        emitter.complete();
                    } catch (Exception e) {
                        log.error("[CreateChatMessageService] Stream error", e);
                        emitter.completeWithError(ChatbotErrorCode.CHATBOT_AI_FAILED.toException());
                    }
                }
            );
        emitter.onCompletion(() -> {
            log.info("[SSE] Connection completed for user: {}", request.getUserId());
            disposable.dispose();
        });

        emitter.onTimeout(() -> {
            log.warn("[SSE] Connection timeout for user: {}", request.getUserId());
            disposable.dispose();
        });

        emitter.onError((e) -> {
            log.error("[SSE] Connection error for user: {}", request.getUserId(), e);
            disposable.dispose();
        });
        return emitter;

    }

    private void saveHistory(String conversationId, List<Message> history, String userMsg, String assistantMsg){
        ArrayList<Message> updated = new ArrayList<>(history);
        updated.add(new UserMessage(userMsg));
        updated.add(new AssistantMessage(assistantMsg));

        if(updated.size() > MAX_MESSAGES){
            updated = new ArrayList<>(updated.subList(updated.size() - MAX_MESSAGES,updated.size()));

        }
        chatMemoryRepository.saveAll(conversationId,updated);
    }

    private String buildQuizContext(ChatMessageRequest request) {
        return "문제: " + request.getQuestion() + "\n정답: " + request.getAnswer();
    }

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
}
