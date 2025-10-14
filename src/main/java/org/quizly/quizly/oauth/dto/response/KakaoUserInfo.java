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
        Object id = attributes.get("id");
        if (id == null) {
            throw new IllegalArgumentException("카카오 API 응답에 'id' 필드가 없습니다.");
        }
        return id.toString();
    }

    @Override
    public String getEmail() {
        if (attributes.get("kakao_account") instanceof Map account) {
            Object email = account.get("email");
            return email != null ? email.toString() : null;
        }
        return null;
    }

    @Override
    public String getName() {
        if (attributes.get("properties") instanceof Map props) {
            Object nickname = props.get("nickname");
            return nickname != null ? nickname.toString() : null;
        }
        return null;
    }
}
