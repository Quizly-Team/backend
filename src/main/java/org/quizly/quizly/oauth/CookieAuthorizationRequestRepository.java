package org.quizly.quizly.oauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieAuthorizationRequestRepository
    implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

  private static final String AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
  private static final int COOKIE_EXPIRE_SECONDS = 180;

  private final ObjectMapper objectMapper;

  @Override
  public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
    return getCookie(request, AUTHORIZATION_REQUEST_COOKIE_NAME)
        .map(cookie -> deserialize(cookie.getValue()))
        .orElse(null);
  }

  @Override
  public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
      HttpServletRequest request, HttpServletResponse response) {
    if (authorizationRequest == null) {
      deleteCookie(response);
      return;
    }
    addCookie(response, serialize(authorizationRequest));
  }

  @Override
  public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
      HttpServletResponse response) {
    OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
    if (authorizationRequest != null) {
      deleteCookie(response);
    }
    return authorizationRequest;
  }

  private void addCookie(HttpServletResponse response, String value) {
    ResponseCookie cookie = ResponseCookie.from(AUTHORIZATION_REQUEST_COOKIE_NAME, value)
        .httpOnly(true)
        .secure(true)
        .path("/")
        .sameSite("None")
        .maxAge(COOKIE_EXPIRE_SECONDS)
        .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  private void deleteCookie(HttpServletResponse response) {
    ResponseCookie cookie = ResponseCookie.from(AUTHORIZATION_REQUEST_COOKIE_NAME, "")
        .httpOnly(true)
        .secure(true)
        .path("/")
        .sameSite("None")
        .maxAge(0)
        .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  private Optional<Cookie> getCookie(HttpServletRequest request, String name) {
    if (request.getCookies() == null) {
      return Optional.empty();
    }
    return Arrays.stream(request.getCookies())
        .filter(cookie -> name.equals(cookie.getName()))
        .findFirst();
  }

  private record AuthRequestData(
      String authorizationUri,
      String clientId,
      String redirectUri,
      Set<String> scopes,
      String state,
      Map<String, Object> additionalParameters,
      Map<String, Object> attributes,
      String authorizationRequestUri
  ) {}

  private String serialize(OAuth2AuthorizationRequest request) {
    AuthRequestData data = new AuthRequestData(
        request.getAuthorizationUri(),
        request.getClientId(),
        request.getRedirectUri(),
        request.getScopes(),
        request.getState(),
        request.getAdditionalParameters(),
        request.getAttributes(),
        request.getAuthorizationRequestUri()
    );
    try {
      return objectMapper.writeValueAsString(data);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("OAuth2 인가 요청 직렬화 실패", e);
    }
  }

  private OAuth2AuthorizationRequest deserialize(String value) {
    try {
      AuthRequestData data = objectMapper.readValue(value, AuthRequestData.class);
      return OAuth2AuthorizationRequest.authorizationCode()
          .authorizationUri(data.authorizationUri())
          .clientId(data.clientId())
          .redirectUri(data.redirectUri())
          .scopes(data.scopes())
          .state(data.state())
          .additionalParameters(data.additionalParameters())
          .attributes(data.attributes())
          .authorizationRequestUri(data.authorizationRequestUri())
          .build();
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("OAuth2 인가 요청 역직렬화 실패", e);
    }
  }
}
