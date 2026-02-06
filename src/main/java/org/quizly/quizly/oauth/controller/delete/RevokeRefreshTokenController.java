package org.quizly.quizly.oauth.controller.delete;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.quizly.quizly.oauth.service.RevokeRefreshTokenService;
import org.quizly.quizly.oauth.service.RevokeRefreshTokenService.RevokeRefreshTokenErrorCode;
import org.quizly.quizly.oauth.service.RevokeRefreshTokenService.RevokeRefreshTokenRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증")
public class RevokeRefreshTokenController {

  private final RevokeRefreshTokenService revokeRefreshTokenService;

  @Operation(
      summary = "리프레시 토큰 무효화 API (로그아웃)",
      description = "인증된 사용자의 리프레시 토큰을 무효화하고 쿠키를 삭제합니다.",
      operationId = "/auth/logout"
  )
  @DeleteMapping("/auth/logout")
  @ApiErrorCode(errorCodes = {GlobalErrorCode.class, RevokeRefreshTokenErrorCode.class})
  public ResponseEntity<Void> revokeRefreshToken(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      HttpServletResponse response
  ) {

    var serviceResponse = revokeRefreshTokenService.execute( RevokeRefreshTokenRequest.builder()
        .userId(userPrincipal.getUserId())
        .build());

    if (!serviceResponse.isSuccess()) {
      Optional.of(serviceResponse)
          .map(BaseResponse::getErrorCode)
          .ifPresent(errorCode -> {
            throw errorCode.toException();
          });
    }

    log.info("[AUTH] Logout - userId: {}", userPrincipal.getUserId());

    ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
        .httpOnly(true)
        .secure(true)
        .path("/")
        .sameSite("Lax")
        .maxAge(0)
        .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

    return ResponseEntity.ok().build();
  }
}