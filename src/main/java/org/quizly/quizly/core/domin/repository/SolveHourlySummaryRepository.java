package org.quizly.quizly.core.domin.repository;

import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.entity.SolveHourlySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SolveHourlySummaryRepository extends JpaRepository<SolveHourlySummary, Long> {

  @Query("SELECT shs.date as date, SUM(shs.solvedCount) as solvedCount " +
      "FROM SolveHourlySummary shs " +
      "WHERE shs.user = :user " +
      "AND shs.date BETWEEN :startDate AND :endDate " +
      "GROUP BY shs.date " +
      "ORDER BY shs.date")
  List<DailySummary> findDailySummaryByUserAndDateBetween(
      @Param("user") User user,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate
  );

  Optional<SolveHourlySummary> findByUserAndDateAndHour(
      User user,
      LocalDate date,
      Integer hour
  );

  interface DailySummary {
    LocalDate getDate();
    Long getSolvedCount();
  }
}
