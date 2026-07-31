package org.quizly.quizly.oauth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quizly.quizly.core.domain.entity.User;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.quizly.quizly.oauth.dto.response.KakaoUserInfo;
import org.quizly.quizly.oauth.dto.response.NaverUserInfo;
import org.quizly.quizly.oauth.dto.response.OAuth2UserInfo;
import org.quizly.quizly.oauth.service.RegisterOAuthUserService.RegisterOAuthUserRequest;
import org.quizly.quizly.oauth.service.RegisterOAuthUserService.RegisterOAuthUserResponse;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginUserService extends DefaultOAuth2UserService {

    private final RegisterOAuthUserService registerOAuthUserService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("[OAuth2LoginUserService] OAuth2 user loading - provider: {}", registrationId);
        OAuth2UserInfo oAuth2UserInfo = getOAuth2UserInfo(registrationId, oAuth2User);
        if (oAuth2UserInfo == null) {
            return null;
        }

        RegisterOAuthUserResponse response = registerOAuthUserService.execute(
            RegisterOAuthUserRequest.builder().oAuth2UserInfo(oAuth2UserInfo).build());

        if (!response.isSuccess()) {
            throw response.getErrorCode() != null
                ? response.getErrorCode().toException()
                : GlobalErrorCode.INTERNAL_ERROR.toException();
        }

        User user = response.getUser();

        return new UserPrincipal(user.getId(), user.getRole());
    }

    private OAuth2UserInfo getOAuth2UserInfo(String registrationId, OAuth2User oAuth2User) {
        switch (registrationId) {
            case "naver":
                return new NaverUserInfo(oAuth2User.getAttributes());
            case "kakao":
                return new KakaoUserInfo(oAuth2User.getAttributes());
            default:
                return null;
        }
    }
}
