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
import org.quizly.quizly.core.domin.repository.RefreshTokenRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.oauth.service.RevokeRefreshTokenService.RevokeRefreshTokenRequest;
import org.quizly.quizly.oauth.service.RevokeRefreshTokenService.RevokeRefreshTokenResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RevokeRefreshTokenService implements BaseService<RevokeRefreshTokenRequest, RevokeRefreshTokenResponse> {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public RevokeRefreshTokenResponse execute(RevokeRefreshTokenRequest revokeRefreshTokenRequest) {
        Long userId = revokeRefreshTokenRequest.getUserId();

        if (!revokeRefreshTokenRequest.isValid()) {
            return RevokeRefreshTokenResponse.builder()
                .success(false)
                .errorCode(RevokeRefreshTokenErrorCode.USER_ID_MISSING)
                .build();
        }

        refreshTokenRepository.deleteByUserId(userId);

        return RevokeRefreshTokenResponse.builder()
            .success(true)
            .build();
    }

    @Getter
    @RequiredArgsConstructor
    public enum RevokeRefreshTokenErrorCode implements BaseErrorCode<DomainException> {
        USER_ID_MISSING(HttpStatus.BAD_REQUEST, "사용자 인증 정보가 제공되지 않았습니다.");

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
    public static class RevokeRefreshTokenRequest implements BaseRequest {
        private Long userId;

        @Override
        public boolean isValid() {
            return userId != null;
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @ToString
    public static class RevokeRefreshTokenResponse extends BaseResponse<RevokeRefreshTokenErrorCode> {
    }
}