package org.quizly.quizly.core.domin.repository;

import org.quizly.quizly.core.domin.entity.Quiz.QuizType;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.entity.SolveTypeSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SolveTypeSummaryRepository extends JpaRepository<SolveTypeSummary, Long> {

  @Query("SELECT sts FROM SolveTypeSummary sts WHERE sts.user = :user AND sts.date BETWEEN :startDate AND :endDate")
  List<SolveTypeSummary> findByUserAndDateBetween(
      @Param("user") User user,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate
  );

  Optional<SolveTypeSummary> findByUserAndQuizTypeAndDate(
      User user,
      QuizType quizType,
      LocalDate date
  );
}