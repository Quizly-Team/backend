package org.quizly.quizly.external.slack.service.sender;

import java.util.Optional;
import org.quizly.quizly.core.notification.NotificationChannel;

public interface SlackMessageSender {

    boolean supports(NotificationChannel channel);

    Optional<String> send(String text, String threadTs);
}
