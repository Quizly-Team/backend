package org.quizly.quizly.chatbot.controller.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.chatbot.dto.request.CreateChatMessageRequest;
import org.quizly.quizly.chatbot.dto.response.CreateChatMessageResponse;
import org.quizly.quizly.chatbot.service.CreateChatMessageService;
import org.quizly.quizly.chatbot.service.CreateChatMessageService.ChatbotErrorCode;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Chatbot", description = "AI 챗봇")
public class CreateChatMessageController {

    private final CreateChatMessageService createChatMessageService;

    @Operation(
        summary = "챗봇 메시지 전송 API",
        description = "모아보기 페이지에서 퀴즈 관련 질문을 AI 챗봇에게 전달합니다.\n\n"
            + "동일한 conversationId로 요청하면 멀티턴 대화가 가능합니다.",
        operationId = "/chatbot/messages"
    )
    @PostMapping("/chatbot/messages")
    @ApiErrorCode(errorCodes = {GlobalErrorCode.class, ChatbotErrorCode.class})
    public ResponseEntity<CreateChatMessageResponse> createChatMessage(
        @RequestBody CreateChatMessageRequest request,
        @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CreateChatMessageService.ChatMessageResponse serviceResponse = createChatMessageService.execute(
            CreateChatMessageService.ChatMessageRequest.builder()
                .conversationId(request.getConversationId())
                .question(request.getQuestion())
                .answer(request.getAnswer())
                .userMessage(request.getUserMessage())
                .userId(userPrincipal.getUserId())
                .build()
        );

        if (serviceResponse == null || !serviceResponse.isSuccess()) {
            Optional.ofNullable(serviceResponse)
                .map(BaseResponse::getErrorCode)
                .ifPresentOrElse(errorCode -> {
                    throw errorCode.toException();
                }, () -> {
                    throw GlobalErrorCode.INTERNAL_ERROR.toException();
                });
        }

        return ResponseEntity.ok(
            CreateChatMessageResponse.builder()
                .conversationId(serviceResponse.getConversationId())
                .assistantMessage(serviceResponse.getAssistantMessage())
                .build()
        );
    }
}
