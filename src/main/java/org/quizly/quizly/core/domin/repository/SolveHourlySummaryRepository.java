package org.quizly.quizly.core.domin.repository;

import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.entity.SolveHourlySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface SolveHourlySummaryRepository extends JpaRepository<SolveHourlySummary, Long> {
  Optional<SolveHourlySummary> findByUserAndDateAndHour(
      User user,
      LocalDate date,
      Integer hour
  );
}
