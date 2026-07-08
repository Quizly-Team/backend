package org.quizly.quizly.oauth;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@Slf4j
public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

  private static final String AUTHORIZATION_BASE_URI = "/oauth2/authorization";

  private final DefaultOAuth2AuthorizationRequestResolver delegate;
  private final List<String> allowedRedirectOrigins;

  public CustomAuthorizationRequestResolver(
      ClientRegistrationRepository clientRegistrationRepository,
      List<String> allowedRedirectOrigins) {
    this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
        clientRegistrationRepository, AUTHORIZATION_BASE_URI);
    this.allowedRedirectOrigins = allowedRedirectOrigins;
  }

  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
    OAuth2AuthorizationRequest authorizationRequest = delegate.resolve(request);
    return customize(request, authorizationRequest);
  }

  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
    OAuth2AuthorizationRequest authorizationRequest = delegate.resolve(request, clientRegistrationId);
    return customize(request, authorizationRequest);
  }

  private OAuth2AuthorizationRequest customize(HttpServletRequest request, OAuth2AuthorizationRequest authorizationRequest) {
    if (authorizationRequest == null) {
      return null;
    }

    String origin = extractOrigin(request);
    if (origin == null || !allowedRedirectOrigins.contains(origin)) {
      return authorizationRequest;
    }

    String registrationId = extractRegistrationId(request);
    if (registrationId == null) {
      return authorizationRequest;
    }

    String customRedirectUri = origin + "/login/oauth2/code/" + registrationId;
    log.debug("[CustomAuthorizationRequestResolver] origin 감지 - redirectUri: {}", customRedirectUri);

    return OAuth2AuthorizationRequest.from(authorizationRequest)
        .redirectUri(customRedirectUri)
        .build();
  }

  private String extractOrigin(HttpServletRequest request) {
    String origin = request.getHeader("Origin");
    if (origin != null && !origin.isBlank()) {
      return origin;
    }

    String referer = request.getHeader("Referer");
    if (referer != null && !referer.isBlank()) {
      try {
        URI uri = new URI(referer);
        int port = uri.getPort();
        return uri.getScheme() + "://" + uri.getHost() + (port != -1 ? ":" + port : "");
      } catch (Exception e) {
        log.warn("[CustomAuthorizationRequestResolver] Referer 파싱 실패: {}", referer);
      }
    }

    return null;
  }

  private String extractRegistrationId(HttpServletRequest request) {
    String uri = request.getRequestURI();
    String prefix = AUTHORIZATION_BASE_URI + "/";
    int idx = uri.indexOf(prefix);
    if (idx < 0) {
      return null;
    }
    return uri.substring(idx + prefix.length());
  }
}
