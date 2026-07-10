package org.quizly.quizly.external.slack.dto.Request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SlackPostMessageRequest(
    String channel,
    String text,
    @JsonProperty("thread_ts") String threadTs) {

}
