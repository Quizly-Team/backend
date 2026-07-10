package org.quizly.quizly.external.slack.service;

import java.util.Optional;
import org.quizly.quizly.core.notification.NotificationMessage;
import org.quizly.quizly.core.notification.NotificationProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile({"local"})
@Service
public class NoOpSlackNotificationService implements NotificationProvider {

    @Override
    public Optional<String> send(NotificationMessage message) {
        return Optional.empty();
    }
}
