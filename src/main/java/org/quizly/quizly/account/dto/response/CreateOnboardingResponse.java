package org.quizly.quizly.account.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.account.service.CreateOnboardingService.CreateOnboardingErrorCode;
import org.quizly.quizly.core.application.BaseResponse;

@Getter
@SuperBuilder
@NoArgsConstructor
@ToString
public class CreateOnboardingResponse
        extends BaseResponse<CreateOnboardingErrorCode> {

    private Boolean onboardingCompleted;
}
