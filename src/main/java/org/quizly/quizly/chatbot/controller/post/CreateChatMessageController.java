package org.quizly.quizly.chatbot.controller.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.chatbot.dto.request.CreateChatMessageRequest;
import org.quizly.quizly.chatbot.service.CreateChatMessageService;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@Tag(name = "Chatbot", description = "AI 챗봇")
public class CreateChatMessageController {

    private final CreateChatMessageService createChatMessageService;

    @Operation(
        summary = "챗봇 메시지 스트리밍 API",
        description = "SSE를 통해 챗봇 답변을 실시간 스트리밍으로 전달합니다.",
        operationId = "/chatbot/messages/stream"
    )
    @ApiResponse(
        responseCode = "200",
        description = "스트리밍 응답 성공",
        content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)
    )
    @PostMapping(value = "/chatbot/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChatMessage(
        @Valid @RequestBody CreateChatMessageRequest request,
        @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return createChatMessageService.execute(
            CreateChatMessageService.ChatMessageRequest.builder()
                .conversationId(request.getConversationId())
                .question(request.getQuestion())
                .answer(request.getAnswer())
                .userMessage(request.getUserMessage())
                .userId(userPrincipal.getUserId())
                .build()
        );
    }
}
