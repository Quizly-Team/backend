package org.quizly.quizly.core.domin.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.domin.shared.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "user")
public class User extends BaseEntity {

  public enum Provider {
    NAVER,
    GOOGLE,
    KAKAO
  }

  @Getter
  @RequiredArgsConstructor
  public enum Role {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN");

    private final String key;

    public static Role fromKey(String key) {
      for (Role role : Role.values()) {
        if (role.getKey().equals(key)) {
          return role;
        }
      }
      throw new IllegalArgumentException("Invalid role key: " + key);
    }
  }

  @Enumerated(EnumType.STRING)
  @Column(nullable = true)
  private Provider provider;

  @Column(nullable = true, unique = true)
  private String providerId;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Quiz> quizList = new ArrayList<>();

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SolveHistory> solveHistories = new ArrayList<>();
}
