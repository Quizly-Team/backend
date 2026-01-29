package org.quizly.quizly.core.notification;

public interface NotificationProvider {
    void send(NotificationMessage message);
}
