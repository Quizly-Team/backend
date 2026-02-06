package org.quizly.quizly.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quizly.quizly.core.domin.entity.RefreshToken;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.repository.RefreshTokenRepository;
import org.quizly.quizly.core.domin.repository.UserRepository;
import org.quizly.quizly.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  private final JwtProvider jwtProvider;
  private final RefreshTokenRepository refreshTokenRepository;
  private final UserRepository userRepository;
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

    Long userId = customUserDetails.getUserId();

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalStateException("User not found"));

    String role = authentication.getAuthorities().stream()
        .findFirst()
        .map(GrantedAuthority::getAuthority)
        .orElseThrow(() -> new IllegalStateException("User has no authorities"));

    String accessToken = jwtProvider.generateAccessToken(userId, role);
    String refreshToken = jwtProvider.generateRefreshToken(userId);

    log.info("[OAuth2LoginSuccessHandler] Login successful - userId: {}, provider: {}", user.getId(), user.getProvider());

    Optional<RefreshToken> refreshTokenOptional = refreshTokenRepository.findByUserId(userId);
    if (refreshTokenOptional.isPresent()) {
      refreshTokenOptional.get().setToken(refreshToken);
    } else {
      refreshTokenRepository.save(new RefreshToken(userId, refreshToken));
    }

    ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
        .httpOnly(true)
        .secure(true)
        .path("/")
        .sameSite("Lax")
        .maxAge(refreshTokenExpiration / 1000)
        .build();

    Map<String, String> tokenMap = new HashMap<>();
    tokenMap.put("accessToken", accessToken);

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(tokenMap));
  }
}
