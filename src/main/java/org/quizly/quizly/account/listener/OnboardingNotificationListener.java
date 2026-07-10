package org.quizly.quizly.account.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.account.event.OnboardingCompletedEvent;
import org.quizly.quizly.account.message.OnboardingNotificationMessage;
import org.quizly.quizly.core.domain.entity.User;
import org.quizly.quizly.core.notification.NotificationProvider;
import org.quizly.quizly.core.notification.NotificationThreadRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Log4j2
@Component
@RequiredArgsConstructor
public class OnboardingNotificationListener {

    private final NotificationProvider notificationProvider;
    private final NotificationThreadRepository notificationThreadRepository;

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OnboardingCompletedEvent event) {
        User user = event.user();
        Long userId = user.getId();
        try {
            String threadTs = notificationThreadRepository.find(userId).orElse(null);
            notificationProvider.send(new OnboardingNotificationMessage(user, threadTs));
            notificationThreadRepository.delete(userId);
        } catch (Exception e) {
            log.warn("[OnboardingNotificationListener] 온보딩 완료 슬랙 알림 전송 실패. userId: {}", userId, e);
        }
    }
}
