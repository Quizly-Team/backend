package org.quizly.quizly.core.domin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
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
@Table(name = "faq")
public class Faq extends BaseEntity {

  @Getter
  @RequiredArgsConstructor
  public enum FaqCategory {
    SERVICE_INTRO("서비스 소개"),
    QUIZ_GENERATION("문제 생성"),
    WRONG_ANSWER("오답 관리"),
    TECH_SUPPORT("기술 지원");

    private final String description;
  }

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FaqCategory category;

  @Column(nullable = false)
  private String question;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String answer;

  public void softDelete() {
    setDeleted(true);
  }
}
