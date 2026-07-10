package org.quizly.quizly.account.event;

import org.quizly.quizly.core.domain.entity.User;

public record OnboardingCompletedEvent(User user) {

}
