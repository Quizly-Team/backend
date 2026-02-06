package org.quizly.quizly.oauth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import lombok.Getter;
import org.quizly.quizly.core.domin.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Getter
public class UserPrincipal implements OAuth2User {

  private final Long userId;
  private final User.Role role;

  public UserPrincipal(Long userId, User.Role role) {
    this.userId = userId;
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
    return String.valueOf(userId);
  }

}