package org.quizly.quizly.external.slack.service;

import org.quizly.quizly.core.notification.NotificationMessage;
import org.quizly.quizly.core.notification.NotificationProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile({"local"})
@Service
public class NoOpSlackNotificationService implements NotificationProvider {
    @Override
    public void send(NotificationMessage message) {

    }
}
