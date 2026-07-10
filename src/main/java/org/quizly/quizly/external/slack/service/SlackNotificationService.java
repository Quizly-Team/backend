package org.quizly.quizly.external.slack.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quizly.quizly.core.notification.NotificationMessage;
import org.quizly.quizly.core.notification.NotificationProvider;
import org.quizly.quizly.external.slack.service.sender.SlackMessageSender;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Profile({"dev", "prod"})
@Slf4j
@Service
@RequiredArgsConstructor
public class SlackNotificationService implements NotificationProvider {

    private final List<SlackMessageSender> senders;
    private final Environment environment;

    @Override
    public Optional<String> send(NotificationMessage message) {
        if (message == null) {
            return Optional.empty();
        }

        SlackMessageSender sender = senders.stream()
            .filter(s -> s.supports(message.channel()))
            .findFirst()
            .orElse(null);

        if (sender == null) {
            log.warn("[SlackNotificationService] 지원하지 않는 알림 채널입니다: {}", message.channel());
            return Optional.empty();
        }

        return sender.send(format(message), message.threadTs());
    }

    private String format(NotificationMessage message) {
        String[] profiles = environment.getActiveProfiles();
        String profile = profiles.length > 0 ? profiles[0].toUpperCase() : "UNKNOWN";
        return "[" + profile + "] [" + message.title() + "]\n" + message.body();
    }
}
