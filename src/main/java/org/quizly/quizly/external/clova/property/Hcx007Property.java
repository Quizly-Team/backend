package org.quizly.quizly.external.clova.property;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties("clova.hcx007")
public class Hcx007Property {

  private String url;

  private String key;
}
