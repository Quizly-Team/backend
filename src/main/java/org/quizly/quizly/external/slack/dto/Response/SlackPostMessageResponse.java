package org.quizly.quizly.external.slack.dto.Response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SlackPostMessageResponse(boolean ok, String ts, String error) {

}
