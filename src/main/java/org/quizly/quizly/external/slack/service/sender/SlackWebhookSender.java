package org.quizly.quizly.external.slack.service.sender;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.quizly.quizly.core.notification.NotificationChannel;
import org.quizly.quizly.external.slack.dto.Request.SlackRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Profile({"dev", "prod"})
@Slf4j
@Component
public class SlackWebhookSender implements SlackMessageSender {

    private final RestTemplate restTemplate;
    private final String batchWebhookUrl;

    public SlackWebhookSender(
        RestTemplate restTemplate,
        @Value("${notification.slack.batch.webhook-url}") String batchWebhookUrl) {
        this.restTemplate = restTemplate;
        this.batchWebhookUrl = batchWebhookUrl;
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.BATCH;
    }

    @Override
    public Optional<String> send(String text, String threadTs) {
        try {
            restTemplate.postForEntity(batchWebhookUrl, new SlackRequest(text), String.class);
        } catch (RestClientException e) {
            log.warn("[SlackWebhookSender] 전송 실패", e);
        }
        return Optional.empty();
    }
}
