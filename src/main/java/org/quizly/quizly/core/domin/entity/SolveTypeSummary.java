package org.quizly.quizly.core.domin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.domin.entity.Quiz.QuizType;
import org.quizly.quizly.core.domin.shared.BaseEntity;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "solve_type_summary", uniqueConstraints = { @UniqueConstraint(columnNames = {"user_id", "quiz_type", "date"}) })
public class SolveTypeSummary extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private QuizType quizType;

  @Column(name = "date", nullable = false)
  private LocalDate date;

  @Column(nullable = false)
  private Integer solvedCount = 0;

  @Column(nullable = false)
  private Integer correctCount = 0;
}