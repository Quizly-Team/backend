package org.quizly.quizly.account.controller.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.account.dto.request.CreateOnboardingRequest;
import org.quizly.quizly.account.dto.response.CreateOnboardingResponse;
import org.quizly.quizly.account.service.CreateOnboardingService;
import org.quizly.quizly.account.service.CreateOnboardingService.CreateOnboardingErrorCode;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Account", description = "계정")
public class CreateOnboardingController {

    private final CreateOnboardingService createOnboardingService;
    @Operation(
        summary = "온보딩 설문조사 API",
        description = "최초 로그인 시 설문 조사 결과를 저장합니다.",
        operationId = "/account"
    )
    @PostMapping("account/onboarding")
    @ApiErrorCode(errorCodes = {GlobalErrorCode.class, CreateOnboardingErrorCode.class})
    public ResponseEntity<CreateOnboardingResponse> onboarding(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody CreateOnboardingRequest request
    ) {
        createOnboardingService.execute(
            CreateOnboardingService.CreateOnboardingRequest.builder()
                .userPrincipal(userPrincipal)
                .targetType(request.getTargetType())
                .studyGoal(request.getStudyGoal())
                .build()
        );

        return ResponseEntity.ok(toResponse());
    }

    private CreateOnboardingResponse toResponse() {
        return CreateOnboardingResponse.builder()
            .onboardingCompleted(true)
            .build();
    }
}

