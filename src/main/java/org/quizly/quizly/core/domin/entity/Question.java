package org.quizly.quizly.core.domin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import org.quizly.quizly.core.domin.shared.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "question")
public class Question extends BaseEntity {

  @Column(nullable = false)
  private String questionText;

  @Column(nullable = false)
  private String answer;

  @Column(nullable = false)
  private String questionType;

  @Column(nullable = false)
  private String explanation;

  @Column(nullable = false)
  private String topic;

  @Builder.Default
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
  @Column(name = "option_text")
  private List<String> options = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = true) // 비회원은 null
  private User user;

  @Column(nullable = false)
  private Boolean guest = false;

  @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SolveHistory> solveHistories = new ArrayList<>();
}
