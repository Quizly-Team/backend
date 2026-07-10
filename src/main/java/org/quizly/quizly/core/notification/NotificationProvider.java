package org.quizly.quizly.core.notification;

import java.util.Optional;

public interface NotificationProvider {

    Optional<String> send(NotificationMessage message);
}
