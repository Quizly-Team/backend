package org.quizly.quizly.core.domain.repository;

import java.time.LocalDate;
import java.util.List;
import org.quizly.quizly.core.domain.entity.SolveHourlySummary;
import org.quizly.quizly.core.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    List<SolveHourlySummary> findByUserAndDate(
        User user,
        LocalDate date
    );

    List<SolveHourlySummary> findByUserAndDateBetween(
        User user,
        LocalDate startDate,
        LocalDate endDate
    );

    interface DailySummary {

        LocalDate getDate();

        Long getSolvedCount();
    }
}
