package org.quizly.quizly.account.service;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.account.service.DeleteUserService.DeleteUserRequest;
import org.quizly.quizly.account.service.DeleteUserService.DeleteUserResponse;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domain.entity.RefreshToken;
import org.quizly.quizly.core.domain.entity.User;
import org.quizly.quizly.core.domain.repository.RefreshTokenRepository;
import org.quizly.quizly.core.domain.repository.UserRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.jwt.JwtProvider;
import org.quizly.quizly.jwt.error.AuthErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class DeleteUserService implements
    BaseService<DeleteUserRequest, DeleteUserResponse> {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    @Override
    public DeleteUserResponse execute(DeleteUserRequest request) {
        if (request == null || !request.isValid()) {
            return DeleteUserResponse.builder()
                .success(false)
                .errorCode(DeleteUserErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
                .build();
        }

        Long userId = request.getUserPrincipal().getUserId();
        if (userId == null) {
            return DeleteUserResponse.builder()
                .success(false)
                .errorCode(DeleteUserErrorCode.NOT_EXIST_PROVIDER_ID)
                .build();
        }

        DeleteUserErrorCode refreshTokenErrorCode = validateRefreshToken(userId,
            request.getRefreshToken());
        if (refreshTokenErrorCode != null) {
            return DeleteUserResponse.builder()
                .success(false)
                .errorCode(refreshTokenErrorCode)
                .build();
        }

        return userRepository.findByIdAndDeletedFalse(userId)
            .map(user -> {
                deleteUser(user, userId);
                DeleteUserResponse response = DeleteUserResponse.builder().build();
                return response;
            })
            .orElseGet(() -> {
                log.warn("[DeleteUserService] User not found for userId: {}", userId);
                DeleteUserResponse response = DeleteUserResponse.builder()
                    .success(false)
                    .errorCode(DeleteUserErrorCode.NOT_FOUND_USER)
                    .build();
                return response;
            });
    }

    private DeleteUserErrorCode validateRefreshToken(Long userId, String refreshToken) {
        AuthErrorCode tokenErrorCode = jwtProvider.validateToken(refreshToken);
        if (tokenErrorCode != null) {
            if (tokenErrorCode == AuthErrorCode.EXPIRED_ACCESS_TOKEN) {
                return DeleteUserErrorCode.REFRESH_TOKEN_EXPIRED;
            }
            return DeleteUserErrorCode.REFRESH_TOKEN_INVALID;
        }

        if (!userId.equals(jwtProvider.getUserId(refreshToken))) {
            log.warn("[DeleteUserService] AT/RT userId mismatch detected - userId: {}", userId);
            return DeleteUserErrorCode.REFRESH_TOKEN_INVALID;
        }

        Optional<RefreshToken> refreshTokenOptional = refreshTokenRepository.findByUserId(userId);
        if (refreshTokenOptional.isEmpty()) {
            return DeleteUserErrorCode.REFRESH_TOKEN_NOT_FOUND;
        }

        if (!refreshTokenOptional.get().getToken().equals(refreshToken)) {
            log.warn("[DeleteUserService] Refresh token mismatch detected - userId: {}", userId);
            return DeleteUserErrorCode.REFRESH_TOKEN_INVALID;
        }

        return null;
    }

    private void deleteUser(User user, Long userId) {
        user.softDelete();
        user.deletePersonalInfo();
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Getter
    @RequiredArgsConstructor
    public enum DeleteUserErrorCode implements BaseErrorCode<DomainException> {

        NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
        NOT_EXIST_PROVIDER_ID(HttpStatus.BAD_REQUEST, "사용자 인증 정보가 제공되지 않았습니다."),
        NOT_FOUND_USER(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
        REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다."),
        REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "리프레시 토큰을 찾을 수 없습니다."),
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
    public static class DeleteUserRequest implements BaseRequest {

        private UserPrincipal userPrincipal;
        private String refreshToken;

        @Override
        public boolean isValid() {
            return userPrincipal != null && refreshToken != null && !refreshToken.isEmpty();
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @ToString
    public static class DeleteUserResponse extends BaseResponse<DeleteUserErrorCode> {

    }
}
