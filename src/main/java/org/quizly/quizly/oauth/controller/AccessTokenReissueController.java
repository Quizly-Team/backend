package org.quizly.quizly.oauth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.oauth.dto.response.AccessTokenReissueResponse;
import org.quizly.quizly.oauth.service.AccessTokenReissueService;
import org.quizly.quizly.oauth.service.AccessTokenReissueService.AccessTokenReissueErrorCode;
import org.quizly.quizly.oauth.service.AccessTokenReissueService.AccessTokenReissueRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증")
public class AccessTokenReissueController {

  private final AccessTokenReissueService accessTokenReissueService;

  @Operation(
      summary = "accessToken 재발급 API",
      description = "유효한 refreshToken으로 accessToken를 재발급합니다.\n\n해당 API는 로그인 시 발급된 refreshToken 쿠키를 자동으로 사용하여 작동합니다.",
      operationId = "/auth/reissue"
  )
  @PostMapping("/auth/reissue")
  @ApiErrorCode(errorCodes = {GlobalErrorCode.class, AccessTokenReissueErrorCode.class})
  public ResponseEntity<AccessTokenReissueResponse> refreshAccessToken(@CookieValue("refreshToken") @Parameter(hidden = true) String refreshToken, HttpServletResponse response) {

    AccessTokenReissueRequest accessTokenReissueRequest = AccessTokenReissueRequest.builder()
        .refreshToken(refreshToken)
        .build();

    AccessTokenReissueService.AccessTokenReissueResponse serviceResponse = accessTokenReissueService.execute(accessTokenReissueRequest);

    if (!serviceResponse.isSuccess()) {
      Optional.of(serviceResponse)
          .map(BaseResponse::getErrorCode)
          .ifPresent(errorCode -> {
            throw errorCode.toException();
          });
    }

    Cookie newRefreshTokenCookie = new Cookie("refreshToken", serviceResponse.getRefreshToken());
    newRefreshTokenCookie.setHttpOnly(true);
    newRefreshTokenCookie.setPath("/");
    response.addCookie(newRefreshTokenCookie);

    return ResponseEntity.ok(
        AccessTokenReissueResponse.builder()
            .accessToken(serviceResponse.getAccessToken())
            .build());
  }
}
