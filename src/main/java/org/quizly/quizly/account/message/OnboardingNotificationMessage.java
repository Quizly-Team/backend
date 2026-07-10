package org.quizly.quizly.account.message;

import org.quizly.quizly.core.domain.entity.User;
import org.quizly.quizly.core.notification.NotificationChannel;
import org.quizly.quizly.core.notification.NotificationMessage;

public class OnboardingNotificationMessage implements NotificationMessage {

    private final User user;
    private final String threadTs;

    public OnboardingNotificationMessage(User user, String threadTs) {
        this.user = user;
        this.threadTs = threadTs;
    }

    @Override
    public String title() {
        return "온보딩 완료";
    }

    @Override
    public String body() {
        String info = "닉네임: " + user.getNickName() + "\n"
            + "study_goal: " + user.getStudyGoal() + "\n"
            + "target_type: " + user.getTargetType();

        if (threadTs == null) {
            return "⚠️ 가입 알림 스레드를 찾지 못했습니다. (7일 경과 또는 가입 알림 누락)\n"
                + info;
        }
        return info;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SIGNUP;
    }

    @Override
    public String threadTs() {
        return threadTs;
    }
}
