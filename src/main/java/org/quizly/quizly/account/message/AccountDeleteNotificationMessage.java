package org.quizly.quizly.account.message;

import lombok.RequiredArgsConstructor;
import org.quizly.quizly.core.domain.entity.User;
import org.quizly.quizly.core.notification.NotificationChannel;
import org.quizly.quizly.core.notification.NotificationMessage;

@RequiredArgsConstructor
public class AccountDeleteNotificationMessage implements NotificationMessage {

    private final Long userId;
    private final String nickName;
    private final User.Provider provider;

    @Override
    public String title() {
        return "회원 탈퇴";
    }

    @Override
    public String body() {
        return "유저가 탈퇴했습니다.\n"
            + "userId: " + userId + "\n"
            + "닉네임: " + nickName + "\n"
            + "provider: " + provider;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SIGNUP;
    }
}
