package org.quizly.quizly.chatbot.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "챗봇 메시지 요청")
public class CreateChatMessageRequest {

    @Schema(description = "대화 식별자", example = "20260504-031712-1")
    private String conversationId;

    @Schema(description = "문제", example = "Java는 컴파일 언어이다")
    private String question;

    @Schema(description = "정답", example = "O")
    private String answer;

    @Schema(description = "사용자 메시지", example = "이 문제 설명해줘")
    private String userMessage;
}
