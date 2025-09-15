package org.quizly.quizly.oauth.dto.response;

public interface OAuth2UserInfo {

  String getProvider();

  String getProviderId();

  String getEmail();

  String getName();
}
