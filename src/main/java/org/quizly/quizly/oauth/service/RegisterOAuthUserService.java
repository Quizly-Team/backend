package org.quizly.quizly.oauth.service;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domain.entity.User;
import org.quizly.quizly.core.domain.entity.User.Provider;
import org.quizly.quizly.core.domain.entity.User.Role;
import org.quizly.quizly.core.domain.repository.UserRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.core.notification.NotificationProvider;
import org.quizly.quizly.core.notification.NotificationThreadRepository;
import org.quizly.quizly.oauth.dto.response.OAuth2UserInfo;
import org.quizly.quizly.oauth.message.SignupNotificationMessage;
import org.quizly.quizly.oauth.service.RegisterOAuthUserService.RegisterOAuthUserRequest;
import org.quizly.quizly.oauth.service.RegisterOAuthUserService.RegisterOAuthUserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth 사용자 정보를 받아온 이후의 DB 조회·생성·수정만 담당하는 서비스.
 *
 * <p>외부 OAuth 통신({@code super.loadUser})은 {@link OAuth2LoginUserService}에서 트랜잭션 밖으로 수행하고,
 * 이 서비스의 {@link #execute}만 트랜잭션으로 감싼다. 외부 응답 지연이 DB 커넥션 점유 시간에 영향을 주지 않도록 분리했다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterOAuthUserService implements
    BaseService<RegisterOAuthUserRequest, RegisterOAuthUserResponse> {

    private final UserRepository userRepository;
    private final NotificationProvider notificationProvider;
    private final NotificationThreadRepository notificationThreadRepository;

    @Override
    @Transactional
    public RegisterOAuthUserResponse execute(RegisterOAuthUserRequest request) {
        OAuth2UserInfo oAuth2UserInfo = request.getOAuth2UserInfo();

        Provider provider;
        try {
            provider = Provider.valueOf(oAuth2UserInfo.getProvider().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("[RegisterOAuthUserService] 지원하지 않는 Provider - provider: {}",
                oAuth2UserInfo.getProvider());
            return RegisterOAuthUserResponse.builder()
                .success(false)
                .errorCode(RegisterOAuthUserErrorCode.UNSUPPORTED_PROVIDER)
                .build();
        }

        User user = userRepository.findByProviderId(oAuth2UserInfo.getProviderId())
            .map(existUser -> updateUser(existUser, oAuth2UserInfo))
            .orElseGet(() -> createUser(provider, oAuth2UserInfo));

        return RegisterOAuthUserResponse.builder()
            .user(user)
            .build();
    }

    private User createUser(Provider provider, OAuth2UserInfo oAuth2UserInfo) {
        User userEntity = new User();
        userEntity.setProvider(provider);
        userEntity.setProviderId(oAuth2UserInfo.getProviderId());
        userEntity.setEmail(oAuth2UserInfo.getEmail());
        userEntity.setName(oAuth2UserInfo.getNickname());
        userEntity.setNickName(oAuth2UserInfo.getNickname());
        userEntity.setRole(Role.USER);
        User savedUser = userRepository.save(userEntity);

        try {
            long totalMemberCount = userRepository.count();
            notificationProvider.send(new SignupNotificationMessage(savedUser, totalMemberCount))
                .ifPresent(threadTs -> notificationThreadRepository.save(savedUser.getId(), threadTs));
        } catch (Exception e) {
            log.warn("[RegisterOAuthUserService] 회원가입 슬랙 알림 전송 실패. userId: {}", savedUser.getId(), e);
        }
        return savedUser;
    }

    private User updateUser(User user, OAuth2UserInfo oAuth2UserInfo) {
        String newEmail = oAuth2UserInfo.getEmail();
        String newName = oAuth2UserInfo.getNickname();

        if (!Objects.equals(user.getEmail(), newEmail)) {
            user.setEmail(newEmail);
        }
        if (!Objects.equals(user.getName(), newName)) {
            user.setName(newName);
        }
        return user;
    }

    @Getter
    @RequiredArgsConstructor
    public enum RegisterOAuthUserErrorCode implements BaseErrorCode<DomainException> {
        UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인 Provider입니다.");

        private final HttpStatus httpStatus;
        private final String message;

        @Override
        public DomainException toException() {
            return new DomainException(httpStatus, this);
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class RegisterOAuthUserRequest implements BaseRequest {

        private OAuth2UserInfo oAuth2UserInfo;

        @Override
        public boolean isValid() {
            return oAuth2UserInfo != null
                && oAuth2UserInfo.getProvider() != null
                && oAuth2UserInfo.getProviderId() != null;
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class RegisterOAuthUserResponse extends BaseResponse<RegisterOAuthUserErrorCode> {

        private User user;
    }
}
