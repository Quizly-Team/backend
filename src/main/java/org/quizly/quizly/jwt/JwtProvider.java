package org.quizly.quizly.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.core.domin.entity.User.Role;
import org.quizly.quizly.jwt.error.AuthErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtProvider {

  @Value("${jwt.secret}")
  private String secret;

  private SecretKey secretKey;

  @Value("${jwt.access-token-expiration}")
  private  Long accessTokenExpiration;

  @Value("${jwt.refresh-token-expiration}")
  private Long refreshTokenExpiration;

  @PostConstruct
  private void init() {
    this.secretKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public String getProviderId(String token) {
    return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload()
        .get("providerId", String.class);
  }

  public String getRole(String token) {
    return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload()
        .get("role", String.class);
  }

  public AuthErrorCode validateToken(String token) {
    try {
      Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
      return null;
    } catch (ExpiredJwtException e) {
      return AuthErrorCode.EXPIRED_ACCESS_TOKEN;
    } catch (JwtException | IllegalArgumentException e) {
      return AuthErrorCode.INVALID_TOKEN;
    }
  }

  public String generateAccessToken(String providerId, String role) {
    return Jwts.builder()
        .claim("providerId", providerId)
        .claim("role", role)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
        .signWith(secretKey)
        .compact();
  }

  public String generateRefreshToken(String providerId) {
    return Jwts.builder()
        .claim("providerId", providerId)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
        .signWith(secretKey)
        .compact();
  }

  public Authentication getAuthentication(String token) {
    String providerId = getProviderId(token);
    String roleString = getRole(token);
    Role role = Role.fromKey(roleString);

    UserPrincipal userPrincipal = new UserPrincipal(providerId, role);
    return new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
  }
}