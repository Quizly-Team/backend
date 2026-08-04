package org.quizly.quizly.oauth.event;

import org.quizly.quizly.core.domain.entity.User;

public record UserSignedUpEvent(User user, long totalMemberCount) {

}
