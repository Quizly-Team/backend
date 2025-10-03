package org.quizly.quizly.oauth.dto.response;

import java.util.Map;


public class KakaoUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getProviderId() {
        return attributes.get("id").toString();
    }

    @Override
    public String getEmail() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        if (account == null) return null;
        Object email = account.get("email");
        return email != null ? email.toString() : null;
    }

    @Override
    public String getName() {
        Map<String, Object> props = (Map<String, Object>) attributes.get("properties");
        if (props == null) return null;
        Object nickname = props.get("nickname");
        return nickname != null ? nickname.toString() : null;
    }
}
