package org.quizly.quizly.external.gemini.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "gemini.api")
public class GeminiProperty {

    private String url;
    private String model;
    private String key;

}
