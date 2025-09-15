package org.quizly.quizly.oauth;

import jakarta.persistence.Column;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.entity.User.Provider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Getter
public class UserPrincipal implements OAuth2User {

  private final Provider provider;
  private final String providerId;
  private final String name;
  private final User.Role role;

  public UserPrincipal(Provider provider, String providerId, String name, User.Role role) {
    this.provider = provider;
    this.providerId = providerId;
    this.name = name;
    this.role = role;
  }

  public UserPrincipal(String providerId, User.Role role) {
    this.provider = null;
    this.providerId = providerId;
    this.name = null;
    this.role = role;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return null;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    Collection<GrantedAuthority> collection = new ArrayList<>();
    collection.add(new GrantedAuthority() {
      @Override
      public String getAuthority() {
        return role.getKey();
      }
    });
    return collection;
  }

  @Override
  public String getName() {
    return name;
  }

}