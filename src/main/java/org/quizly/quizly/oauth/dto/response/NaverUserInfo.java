package org.quizly.quizly.oauth.dto.response;

import java.util.Map;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

public class NaverUserInfo implements OAuth2UserInfo {

  private final Map<String, Object> attribute;

  public NaverUserInfo(Map<String, Object> attribute) {
    Object response = attribute.get("response");
    if (!(response instanceof Map)) {
      throw new OAuth2AuthenticationException(new OAuth2Error("invalid_response",
          "Invalid Naver API response: 'response' field is missing or not a Map.", null));
    }
    this.attribute = (Map<String, Object>) response;
  }

  private String getAttribute(String key) {
    Object value = this.attribute.get(key);
    if (value == null) {
      throw new OAuth2AuthenticationException(new OAuth2Error("invalid_response",
          "Missing required field in Naver API response: " + key, null));
    }
    return value.toString();
  }

  @Override
  public String getProvider() {
    return "naver";
  }

  @Override
  public String getProviderId() {
    return getAttribute("id");
  }

  @Override
  public String getEmail() {
    return getAttribute("email");
  }

  @Override
  public String getName() {
    return getAttribute("name");
  }
}