package org.quizly.quizly.oauth.service;

import java.util.Optional;
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
import org.quizly.quizly.core.domin.repository.RefreshTokenRepository;
import org.quizly.quizly.core.domin.repository.UserRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.jwt.JwtProvider;
import org.quizly.quizly.jwt.error.AuthErrorCode;
import org.quizly.quizly.oauth.service.ReissueAccessTokenService.ReissueAccessTokenRequest;
import org.quizly.quizly.oauth.service.ReissueAccessTokenService.ReissueAccessTokenResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ReissueAccessTokenService implements BaseService<ReissueAccessTokenRequest, ReissueAccessTokenResponse> {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Override
    public ReissueAccessTokenResponse execute(ReissueAccessTokenRequest reissueAccessTokenRequest) {
        String refreshToken = reissueAccessTokenRequest.getRefreshToken();

        AuthErrorCode errorCode = jwtProvider.validateToken(refreshToken);

        if (errorCode != null) {
            if (errorCode == AuthErrorCode.EXPIRED_ACCESS_TOKEN) {
                return ReissueAccessTokenResponse.builder()
                    .success(false)
                    .errorCode(ReissueAccessTokenErrorCode.REFRESH_TOKEN_EXPIRED)
                    .build();
            } else {
                return ReissueAccessTokenResponse.builder()
                    .success(false)
                    .errorCode(ReissueAccessTokenErrorCode.REFRESH_TOKEN_INVALID)
                    .build();
            }
        }

        Long userId = jwtProvider.getUserId(refreshToken);

        Optional<RefreshToken> refreshTokenOptional = refreshTokenRepository.findByUserId(userId);
        if (refreshTokenOptional.isEmpty()) {
            return ReissueAccessTokenResponse.builder()
                .success(false)
                .errorCode(ReissueAccessTokenErrorCode.REFRESH_TOKEN_NOT_FOUND)
                .build();
        }
        RefreshToken savedRefreshToken = refreshTokenOptional.get();

        if (!savedRefreshToken.getToken().equals(refreshToken)) {
            log.warn("[ReissueAccessTokenService] Token mismatch detected - userId: {}", userId);
            return ReissueAccessTokenResponse.builder()
                .success(false)
                .errorCode(ReissueAccessTokenErrorCode.REFRESH_TOKEN_INVALID)
                .build();
        }

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            log.warn("[ReissueAccessTokenService] User Not  - userId: {}", userId);
            return ReissueAccessTokenResponse.builder()
                .success(false)
                .errorCode(ReissueAccessTokenErrorCode.USER_NOT_FOUND)
                .build();
        }
        User user = userOptional.get();

        String role = user.getRole().name();

        String newAccessToken = jwtProvider.generateAccessToken(userId, role);
        String newRefreshToken = jwtProvider.generateRefreshToken(userId);

        savedRefreshToken.setToken(newRefreshToken);

        log.info("[ReissueAccessTokenService] Token reissued - userId: {}", userId);

        return ReissueAccessTokenResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .build();
    }

    @Getter
    @RequiredArgsConstructor
    public enum ReissueAccessTokenErrorCode implements BaseErrorCode<DomainException> {
        REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다."),
        REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "리프레시 토큰을 찾을 수 없습니다."),
        REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
        USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다.");

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
    public static class ReissueAccessTokenRequest implements BaseRequest {
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
    public static class ReissueAccessTokenResponse extends BaseResponse<ReissueAccessTokenErrorCode> {
        private String accessToken;
        private String refreshToken;
    }
}
