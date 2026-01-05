package org.quizly.quizly.core.domin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.domin.shared.BaseEntity;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "solve_hourly_summary",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "date", "hour"})
    }
)
public class SolveHourlySummary extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "date", nullable = false)
  private LocalDate date;

  @Column(name = "hour", nullable = false)
  private int hour;

  @Column(nullable = false)
  private int solvedCount;
}
