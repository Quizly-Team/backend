package org.quizly.quizly.external.slack.service.sender;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.quizly.quizly.core.notification.NotificationChannel;
import org.quizly.quizly.external.slack.dto.Request.SlackPostMessageRequest;
import org.quizly.quizly.external.slack.dto.Response.SlackPostMessageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Profile({"dev", "prod"})
@Slf4j
@Component
public class SlackApiSender implements SlackMessageSender {

    private static final String POST_MESSAGE_URL = "https://slack.com/api/chat.postMessage";

    private final RestTemplate restTemplate;
    private final String botToken;
    private final String channelId;

    public SlackApiSender(
        RestTemplate restTemplate,
        @Value("${notification.slack.signup.bot-token}") String botToken,
        @Value("${notification.slack.signup.channel-id}") String channelId) {
        this.restTemplate = restTemplate;
        this.botToken = botToken;
        this.channelId = channelId;
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.SIGNUP;
    }

    @Override
    public Optional<String> send(String text, String threadTs) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(botToken);

        SlackPostMessageRequest body = new SlackPostMessageRequest(channelId, text, threadTs);
        HttpEntity<SlackPostMessageRequest> httpEntity = new HttpEntity<>(body, headers);

        try {
            SlackPostMessageResponse response =
                restTemplate.postForObject(POST_MESSAGE_URL, httpEntity, SlackPostMessageResponse.class);

            if (response == null || !response.ok()) {
                log.warn(
                    "[SlackApiSender] 전송 실패. error: {}",
                    response == null ? "no response" : response.error());
                return Optional.empty();
            }
            return Optional.ofNullable(response.ts());
        } catch (RestClientException e) {
            log.warn("[SlackApiSender] 전송 실패", e);
            return Optional.empty();
        }
    }
}
