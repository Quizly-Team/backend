package org.quizly.quizly.core.domin.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Table(name = "quiz")
public class Quiz extends BaseEntity {

  @Getter
  @RequiredArgsConstructor
  public enum QuizType {
    MULTIPLE_CHOICE("prompt/basic_quiz/multiple_choice.txt"),
    TRUE_FALSE("prompt/basic_quiz/true_false.txt");

    private final String promptPath;
  }

  @Column(nullable = false)
  private String quizText;

  @Column(nullable = false)
  private String answer;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private QuizType quizType;

  @Column(nullable = false)
  private String explanation;

  @Column(nullable = false)
  private String topic;

  @Builder.Default
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "quiz_options", joinColumns = @JoinColumn(name = "quiz_id"))
  @Column(name = "option_text")
  private List<String> options = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = true) // 비회원은 null
  private User user;

  @Column(nullable = false)
  private Boolean guest = false;

  @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SolveHistory> solveHistories = new ArrayList<>();
}