package org.quizly.quizly.account.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOnboardingRequest {

    private String targetType;

    private String studyGoal;

}
