package org.quizly.quizly.oauth.message;

import org.quizly.quizly.core.domain.entity.User;
import org.quizly.quizly.core.notification.NotificationChannel;
import org.quizly.quizly.core.notification.NotificationMessage;

public class SignupNotificationMessage implements NotificationMessage {

    private final User user;
    private final long totalMemberCount;

    public SignupNotificationMessage(User user, long totalMemberCount) {
        this.user = user;
        this.totalMemberCount = totalMemberCount;
    }

    @Override
    public String title() {
        return "회원 가입";
    }

    @Override
    public String body() {
        return "새로운 유저가 가입했습니다!\n"
            + "누적 가입자: " + totalMemberCount + "명\n"
            + "닉네임: " + user.getNickName() + "\n"
            + "가입 시각: " + user.getCreatedAt() + "\n"
            + "provider: " + user.getProvider();
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SIGNUP;
    }
}
