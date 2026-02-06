package org.quizly.quizly.oauth.dto.response;

import java.util.Map;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;


public class KakaoUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getNestedMap(String key) {
        Object value = attributes.get(key);
        if (!(value instanceof Map)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("invalid_response",
                    "Missing '" + key + "' field in Kakao API response", null));
        }
        return (Map<String, Object>) value;
    }

    private String getNestedAttribute(String parentKey, String childKey) {
        Map<String, Object> parent = getNestedMap(parentKey);
        Object value = parent.get(childKey);
        if (value == null) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("invalid_response",
                    "Missing required field in Kakao API response: " + childKey + " (필수 동의 항목)", null));
        }
        return value.toString();
    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getProviderId() {
        Object id = attributes.get("id");
        if (id == null) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("invalid_response",
                    "Missing required field in Kakao API response: id", null));
        }
        return id.toString();
    }

    @Override
    public String getEmail() {
        return getNestedAttribute("kakao_account", "email");
    }

    @Override
    public String getNickname() {
        return getNestedAttribute("properties", "nickname");
    }
}
