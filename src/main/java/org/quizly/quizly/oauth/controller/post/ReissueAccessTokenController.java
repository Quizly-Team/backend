package org.quizly.quizly.oauth.controller.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.jwt.JwtProvider;
import org.quizly.quizly.oauth.dto.response.ReissueAccessTokenResponse;
import org.quizly.quizly.oauth.service.ReissueAccessTokenService;
import org.quizly.quizly.oauth.service.ReissueAccessTokenService.ReissueAccessTokenErrorCode;
import org.quizly.quizly.oauth.service.ReissueAccessTokenService.ReissueAccessTokenRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증")
public class ReissueAccessTokenController {

  private final ReissueAccessTokenService reissueAccessTokenService;

  @Value("${jwt.refresh-token-expiration}")
  private Long refreshTokenExpiration;

  @Operation(
      summary = "accessToken 재발급 API",
      description = "유효한 refreshToken으로 accessToken를 재발급합니다.\n\n해당 API는 로그인 시 발급된 refreshToken 쿠키를 자동으로 사용하여 작동합니다.",
      operationId = "/auth/reissue"
  )
  @PostMapping("/auth/reissue")
  @ApiErrorCode(errorCodes = {GlobalErrorCode.class, ReissueAccessTokenErrorCode.class})
  public ResponseEntity<ReissueAccessTokenResponse> refreshAccessToken(@CookieValue("refreshToken") @Parameter(hidden = true) String refreshToken, HttpServletResponse response) {

    ReissueAccessTokenRequest reissueAccessTokenRequest = ReissueAccessTokenRequest.builder()
        .refreshToken(refreshToken)
        .build();

    ReissueAccessTokenService.ReissueAccessTokenResponse serviceResponse = reissueAccessTokenService.execute(reissueAccessTokenRequest);

    if (!serviceResponse.isSuccess()) {
      Optional.of(serviceResponse)
          .map(BaseResponse::getErrorCode)
          .ifPresent(errorCode -> {
            throw errorCode.toException();
          });
    }

    String newRefreshToken = serviceResponse.getRefreshToken();

    ResponseCookie cookie = ResponseCookie.from("refreshToken", newRefreshToken)
        .httpOnly(true)
        .secure(true)
        .path("/")
        .sameSite("Lax")
        .maxAge((int) (refreshTokenExpiration / 1000))
        .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

    return ResponseEntity.ok(
        ReissueAccessTokenResponse.builder()
            .accessToken(serviceResponse.getAccessToken())
            .build());
  }
}
