package org.quizly.quizly.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.core.domin.entity.RefreshToken;
import org.quizly.quizly.core.domin.repository.RefreshTokenRepository;
import org.quizly.quizly.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

//  TODO: 프로덕션 환경에서는 secure 플래그를 true로 설정해야 합니다. (하단 주석 코드)
@RequiredArgsConstructor
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  private final JwtProvider jwtProvider;
  private final RefreshTokenRepository refreshTokenRepository;
  private final ObjectMapper objectMapper;

  @Value("${jwt.refresh-token-expiration}")
  private Long refreshTokenExpiration;


  @Override
  @Transactional
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    UserPrincipal customUserDetails = (UserPrincipal) authentication.getPrincipal();

    String providerId = customUserDetails.getProviderId();

    String role = authentication.getAuthorities().stream()
        .findFirst()
        .map(GrantedAuthority::getAuthority)
        .orElseThrow(() -> new IllegalStateException("User has no authorities"));

    String accessToken = jwtProvider.generateAccessToken(providerId, role);
    String refreshToken = jwtProvider.generateRefreshToken(providerId);

    Optional<RefreshToken> refreshTokenOptional = refreshTokenRepository.findByProviderId(providerId);
    if (refreshTokenOptional.isPresent()) {
      refreshTokenOptional.get().setToken(refreshToken);
    } else {
      refreshTokenRepository.save(new RefreshToken(providerId, customUserDetails.getName(), refreshToken));
    }

    Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
    refreshTokenCookie.setHttpOnly(true);
    refreshTokenCookie.setPath("/");
    refreshTokenCookie.setMaxAge((int) (refreshTokenExpiration / 1000));
    // refreshTokenCookie.setSecure(true);

    Map<String, String> tokenMap = new HashMap<>();
    tokenMap.put("accessToken", accessToken);

    response.addCookie(refreshTokenCookie);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(tokenMap));
  }
}
