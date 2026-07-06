package org.quizly.quizly.core.domain.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.quizly.quizly.core.domain.entity.Quiz.QuizType;
import org.quizly.quizly.core.domain.entity.SolveHistory;
import org.quizly.quizly.core.domain.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolveHistoryStatisticsRepository extends JpaRepository<SolveHistory, Long> {

    @Query("SELECT q.quizType as quizType, " +
        "COUNT(sh) as totalCount, " +
        "SUM(CASE WHEN sh.isCorrect = TRUE THEN 1 ELSE 0 END) as correctCount " +
        "FROM SolveHistory sh " +
        "JOIN sh.quiz q " +
        "WHERE sh.user = :user " +
        "AND sh.submittedAt >= :startDateTime " +
        "AND sh.submittedAt < :endDateTime " +
        "AND sh.isFirst = TRUE " +
        "GROUP BY q.quizType")
    List<QuizTypeSummary> findFirstAttemptsByQuizTypeAndDateTimeRange(
        @Param("user") User user,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );

    default List<QuizTypeSummary> findFirstAttemptsByQuizTypeAndDate(User user,
        LocalDate targetDate) {
        LocalDateTime startDateTime = targetDate.atStartOfDay();
        LocalDateTime endDateTime = targetDate.plusDays(1).atStartOfDay();
        return findFirstAttemptsByQuizTypeAndDateTimeRange(user, startDateTime, endDateTime);
    }

    interface QuizTypeSummary {

        QuizType getQuizType();

        Long getTotalCount();

        Long getCorrectCount();
    }

    @Query("""
          SELECT q.topic as topic,
                 COUNT(sh) as totalCount,
                 SUM(CASE WHEN sh.isCorrect = TRUE THEN 1 ELSE 0 END) as correctCount
          FROM SolveHistory sh
          JOIN sh.quiz q
          WHERE sh.user = :user
            AND sh.submittedAt >= :startDateTime
            AND sh.submittedAt < :endDateTime
            AND sh.isFirst = TRUE
          GROUP BY q.topic
        """)
    List<TopicSummary> findMonthlyTopicSummary(
        @Param("user") User user,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime,
        Pageable pageable
    );

    interface TopicSummary {

        String getTopic();

        Long getTotalCount();

        Long getCorrectCount();
    }

    @Query("SELECT CAST(sh.submittedAt AS LocalDate) as date, " +
        "COUNT(sh) as solvedCount " +
        "FROM SolveHistory sh " +
        "WHERE sh.user = :user " +
        "AND sh.submittedAt >= :startDateTime " +
        "AND sh.submittedAt < :endDateTime " +
        "AND sh.isFirst = TRUE " +
        "GROUP BY CAST(sh.submittedAt AS LocalDate) " +
        "ORDER BY CAST(sh.submittedAt AS LocalDate)")
    List<DailySummary> findFirstAttemptsDailySummaryByUserAndDateTimeRange(
        @Param("user") User user,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );

    default List<DailySummary> findFirstAttemptsDailySummaryByUserAndDate(User user,
        LocalDate date) {
        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.plusDays(1).atStartOfDay();
        return findFirstAttemptsDailySummaryByUserAndDateTimeRange(user, startDateTime,
            endDateTime);
    }

    @Query("SELECT FUNCTION('HOUR', sh.submittedAt) as hourOfDay, " +
        "COUNT(sh) as solvedCount " +
        "FROM SolveHistory sh " +
        "WHERE sh.user = :user " +
        "AND sh.submittedAt >= :startDateTime " +
        "AND sh.submittedAt < :endDateTime " +
        "AND sh.isFirst = TRUE " +
        "GROUP BY FUNCTION('HOUR', sh.submittedAt)")
    List<HourlySummary> findFirstAttemptsHourlySummaryByUserAndDateTimeRange(
        @Param("user") User user,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );

    default List<HourlySummary> findFirstAttemptsHourlySummaryByUserAndDate(User user,
        LocalDate date) {
        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.plusDays(1).atStartOfDay();
        return findFirstAttemptsHourlySummaryByUserAndDateTimeRange(user, startDateTime,
            endDateTime);
    }

    interface DailySummary {

        LocalDate getDate();

        Long getSolvedCount();
    }

    interface HourlySummary {

        Integer getHourOfDay();

        Long getSolvedCount();
    }
}
