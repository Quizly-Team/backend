package org.quizly.quizly.external.openai.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "openai.api")
public class OpenAiProperty {
    private String url;
    private String model;
    private String key;
}
