package org.quizly.quizly.oauth.listener;

import lombok.RequiredArgsConstructor;
import org.quizly.quizly.core.domain.entity.User;
import org.quizly.quizly.core.notification.NotificationExecutor;
import org.quizly.quizly.core.notification.NotificationProvider;
import org.quizly.quizly.core.notification.NotificationThreadRepository;
import org.quizly.quizly.oauth.event.UserSignedUpEvent;
import org.quizly.quizly.oauth.message.SignupNotificationMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SignupNotificationListener {

    private final NotificationProvider notificationProvider;
    private final NotificationThreadRepository notificationThreadRepository;
    private final NotificationExecutor notificationExecutor;

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserSignedUpEvent event) {
        User user = event.user();
        Long userId = user.getId();

        notificationExecutor.runQuietly("SignupNotificationListener", userId, () ->
            notificationProvider.send(new SignupNotificationMessage(user, event.totalMemberCount()))
                .ifPresent(threadTs -> notificationThreadRepository.save(userId, threadTs)));
    }
}
