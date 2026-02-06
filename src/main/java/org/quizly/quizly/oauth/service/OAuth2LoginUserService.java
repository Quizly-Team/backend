package org.quizly.quizly.oauth.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.entity.User.Provider;
import org.quizly.quizly.core.domin.entity.User.Role;
import org.quizly.quizly.core.domin.repository.UserRepository;
import org.quizly.quizly.oauth.UserPrincipal;
import org.quizly.quizly.oauth.dto.response.KakaoUserInfo;
import org.quizly.quizly.oauth.dto.response.NaverUserInfo;
import org.quizly.quizly.oauth.dto.response.OAuth2UserInfo;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginUserService extends DefaultOAuth2UserService {

  private final UserRepository userRepository;

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

    OAuth2User oAuth2User = super.loadUser(userRequest);

    String registrationId = userRequest.getClientRegistration().getRegistrationId();
    log.info("[OAuth2LoginUserService] OAuth2 user loading - provider: {}", registrationId);
    OAuth2UserInfo oAuth2UserInfo = getOAuth2UserInfo(registrationId, oAuth2User);
    if (oAuth2UserInfo == null) {
      return null;
    }

    User user = processUser(oAuth2UserInfo);

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

  private User processUser(OAuth2UserInfo oAuth2UserInfo) {
    Optional<User> existDataOptional = userRepository.findByProviderId(oAuth2UserInfo.getProviderId());

    return existDataOptional.map(user -> updateUser(user, oAuth2UserInfo))
        .orElseGet(() -> createUser(oAuth2UserInfo));

  }

  private User createUser(OAuth2UserInfo oAuth2UserInfo) {
    User userEntity = new User();

    try {
      Provider providerEnum = Provider.valueOf(oAuth2UserInfo.getProvider().toUpperCase());
      userEntity.setProvider(providerEnum);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("지원하지 않는 소셜 로그인 Provider입니다: " + oAuth2UserInfo.getProvider());
    }

    userEntity.setProviderId(oAuth2UserInfo.getProviderId());
    userEntity.setEmail(oAuth2UserInfo.getEmail());
    userEntity.setName(oAuth2UserInfo.getNickname());
    userEntity.setNickName(oAuth2UserInfo.getNickname());
    userEntity.setRole(Role.USER);
    userRepository.save(userEntity);
    return userEntity;
  }

  private User updateUser(User user, OAuth2UserInfo oAuth2UserInfo) {
    user.setEmail(oAuth2UserInfo.getEmail());
    user.setName(oAuth2UserInfo.getNickname());
    userRepository.save(user);
    return user;
  }
}
