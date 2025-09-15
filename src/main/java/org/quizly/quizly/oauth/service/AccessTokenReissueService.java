package org.quizly.quizly.oauth.service;

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
import org.quizly.quizly.core.domin.entity.RefreshToken;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.entity.User.Role;
import org.quizly.quizly.core.domin.repository.RefreshTokenRepository;
import org.quizly.quizly.core.domin.repository.UserRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.jwt.JwtProvider;
import org.quizly.quizly.jwt.error.AuthErrorCode;
import org.quizly.quizly.oauth.service.AccessTokenReissueService.AccessTokenReissueRequest;
import org.quizly.quizly.oauth.service.AccessTokenReissueService.AccessTokenReissueResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AccessTokenReissueService implements BaseService<AccessTokenReissueRequest, AccessTokenReissueResponse> {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Override
    public AccessTokenReissueResponse execute(AccessTokenReissueRequest accessTokenReissueRequest) {
        String refreshToken = accessTokenReissueRequest.getRefreshToken();

        AuthErrorCode errorCode = jwtProvider.validateToken(refreshToken);

        if (errorCode != null) {
            if (errorCode == AuthErrorCode.EXPIRED_ACCESS_TOKEN) {
                return AccessTokenReissueResponse.builder()
                    .success(false)
                    .errorCode(AccessTokenReissueErrorCode.REFRESH_TOKEN_EXPIRED)
                    .build();
            } else {
                return AccessTokenReissueResponse.builder()
                    .success(false)
                    .errorCode(AccessTokenReissueErrorCode.REFRESH_TOKEN_INVALID)
                    .build();
            }
        }

        String providerId = jwtProvider.getProviderId(refreshToken);

        User user = userRepository.findByProviderId(providerId);
        if (user == null) {
            log.warn("User not found for providerId derived from a refresh token. ProviderId: {}", providerId);
            return AccessTokenReissueResponse.builder()
                .success(false)
                .errorCode(AccessTokenReissueErrorCode.USER_NOT_FOUND)
                .build();
        }

        String role = user.getRole().getKey();

        RefreshToken refreshTokenEntity = refreshTokenRepository.findByProviderId(providerId);

        if (refreshTokenEntity == null) {
            return AccessTokenReissueResponse.builder()
                .success(false)
                .errorCode(AccessTokenReissueErrorCode.REFRESH_TOKEN_NOT_FOUND)
                .build();
        }
        if (!refreshTokenEntity.getToken().equals(refreshToken)) {
            log.warn("Refresh Token Mismatch Detected. ProviderId: {}", providerId);
            return AccessTokenReissueResponse.builder()
                .success(false)
                .errorCode(AccessTokenReissueErrorCode.REFRESH_TOKEN_INVALID)
                .build();
        }
        String newAccessToken = jwtProvider.generateAccessToken(providerId, role);
        String newRefreshToken = jwtProvider.generateRefreshToken(providerId);

        refreshTokenEntity.setToken(newRefreshToken);

        return AccessTokenReissueResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .build();
    }

    @Getter
    @RequiredArgsConstructor
    public enum AccessTokenReissueErrorCode implements BaseErrorCode<DomainException> {
        REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다."),
        REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "리프레시 토큰을 찾을 수 없습니다."),
        USER_NOT_FOUND(HttpStatus.NOT_FOUND, "토큰에 해당하는 사용자를 찾을 수 없습니다."),
        REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");

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
    public static class AccessTokenReissueRequest implements BaseRequest {
        private String refreshToken;

        @Override
        public boolean isValid() {
            return refreshToken != null && !refreshToken.isEmpty();
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class AccessTokenReissueResponse extends BaseResponse<AccessTokenReissueErrorCode> {
        private String accessToken;
        private String refreshToken;
    }
}
