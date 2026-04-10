package org.quizly.quizly.chatbot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "챗봇 메시지 응답")
public class CreateChatMessageResponse {

    @Schema(description = "대화 식별자")
    private String conversationId;

    @Schema(description = "AI 응답 메시지")
    private String assistantMessage;
}
