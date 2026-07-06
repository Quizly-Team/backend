package org.quizly.quizly.core.domain.repository.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

@Log4j2
@Repository
@RequiredArgsConstructor
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final String KEY_PREFIX = "chatbot:memory:";
    private static final long TTL_HOURS = 1;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<String> findConversationIds() {
        return List.of();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        Assert.hasText(conversationId, "conversationId must not be empty");
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + conversationId);
        if (json == null) {
            return List.of();
        }
        return deserializeMessages(json);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        Assert.hasText(conversationId, "conversationId must not be empty");
        Assert.notNull(messages, "messages must not be null");
        String json = serializeMessages(messages);
        redisTemplate.opsForValue()
            .set(KEY_PREFIX + conversationId, json, TTL_HOURS, TimeUnit.HOURS);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        Assert.hasText(conversationId, "conversationId must not be empty");
        redisTemplate.delete(KEY_PREFIX + conversationId);
    }

    private String serializeMessages(List<Message> messages) {
        List<Map<String, String>> serialized = messages.stream()
            .filter(msg -> msg.getMessageType() != MessageType.SYSTEM)
            .map(msg -> Map.of(
                "type", msg.getMessageType().name(),
                "text", msg.getText()
            ))
            .toList();
        try {
            return objectMapper.writeValueAsString(serialized);
        } catch (JsonProcessingException e) {
            log.error("[RedisChatMemoryRepository] Failed to serialize messages", e);
            return "[]";
        }
    }

    private List<Message> deserializeMessages(String json) {
        try {
            List<Map<String, String>> raw = objectMapper.readValue(json,
                new TypeReference<>() {
                });
            return raw.stream()
                .map(entry -> {
                    MessageType type = MessageType.valueOf(entry.get("type"));
                    String text = entry.get("text");
                    return (Message) switch (type) {
                        case USER -> new UserMessage(text);
                        case ASSISTANT -> new AssistantMessage(text);
                        case SYSTEM -> new SystemMessage(text);
                        default -> {
                            log.warn(
                                "[RedisChatMemoryRepository] Unexpected message type: {}, skipping",
                                type);
                            yield new UserMessage(text);
                        }
                    };
                })
                .toList();
        } catch (JsonProcessingException e) {
            log.error("[RedisChatMemoryRepository] Failed to deserialize messages", e);
            return List.of();
        }
    }
}