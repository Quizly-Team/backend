package org.quizly.quizly.external.slack.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.core.notification.NotificationMessage;
import org.quizly.quizly.core.notification.NotificationProvider;
import org.quizly.quizly.external.slack.dto.Request.SlackRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
@Profile({"dev", "prod"})
@Slf4j
@Service
@RequiredArgsConstructor
public class SlackNotificationService implements NotificationProvider, BaseService<SlackNotificationService.NotificationRequest, SlackNotificationService.NotificationResponse> {

    @Value("${notification.slack.webhook-url}")
    private String webhookUrl;

    private final RestTemplate restTemplate;
    private final Environment environment;

    @Override
    public void send(NotificationMessage message) {
        if (message == null) {
            return;
        }
        String text = format(message);
        this.execute(new NotificationRequest(text));
    }

    private String format(NotificationMessage message) {
        String[] profiles = environment.getActiveProfiles();
        String profile = profiles.length > 0 ? profiles[0].toUpperCase() : "UNKNOWN";
        return "[" + profile + "] [" + message.title() + "]\n" + message.body();
    }
    @Override
    public NotificationResponse execute(NotificationRequest request) {
        if (request == null || !request.isValid()) {
            return NotificationResponse.builder().success(false).build();
        }

        try {
            SlackRequest slackRequest = new SlackRequest(request.getMessage());
            restTemplate.postForEntity(webhookUrl, slackRequest, String.class);

            return NotificationResponse.builder()
                .success(true)
                .build();
        } catch (RestClientException e) {
            log.error("[SlackNotificationService] 전송 실패", e);
            return NotificationResponse.builder()
                .success(false)
                .errorCode(NotificationErrorCode.SEND_FAILED)
                .build();
        }
    }
    @Getter
    @RequiredArgsConstructor
    public enum NotificationErrorCode implements BaseErrorCode<DomainException> {
        SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "알림 전송 중 오류가 발생했습니다.");

        private final HttpStatus httpStatus;
        private final String message;

        @Override
        public DomainException toException() {
            return new DomainException(httpStatus, this);
        }
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NotificationRequest implements BaseRequest {
        private String message;

        @Override
        public boolean isValid() {
            return message != null && !message.isBlank();
        }
    }

    @Getter
    @SuperBuilder
    @NoArgsConstructor
    public static class NotificationResponse extends BaseResponse<NotificationErrorCode> {
    }
}