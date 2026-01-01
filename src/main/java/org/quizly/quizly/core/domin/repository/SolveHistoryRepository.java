package org.quizly.quizly.core.domin.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.quizly.quizly.core.domin.entity.Quiz.QuizType;
import org.quizly.quizly.core.domin.entity.SolveHistory;
import org.quizly.quizly.core.domin.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolveHistoryRepository extends JpaRepository<SolveHistory, Long> {

  @Query("SELECT sh FROM SolveHistory sh LEFT JOIN FETCH sh.quiz WHERE sh.user = :user AND (sh.quiz.id, sh.createdAt) IN (SELECT sh2.quiz.id, MAX(sh2.createdAt) FROM SolveHistory sh2 WHERE sh2.user = :user GROUP BY sh2.quiz.id)")
  List<SolveHistory> findLatestSolveHistoriesByUser(@Param("user") User user);

  @Query("SELECT sh FROM SolveHistory sh LEFT JOIN FETCH sh.quiz WHERE sh.user = :user AND sh.isCorrect = FALSE AND (sh.quiz.id, sh.createdAt) IN (SELECT sh2.quiz.id, MAX(sh2.createdAt) FROM SolveHistory sh2 WHERE sh2.user = :user GROUP BY sh2.quiz.id)")
  List<SolveHistory> findLatestWrongSolveHistoriesByUser(@Param("user") User user);

  @Query("SELECT q.quizType as quizType, " +
      "COUNT(sh) as totalCount, " +
      "SUM(CASE WHEN sh.isCorrect = true THEN 1 ELSE 0 END) as correctCount " +
      "FROM SolveHistory sh " +
      "JOIN sh.quiz q " +
      "WHERE sh.user = :user " +
      "AND sh.submittedAt >= :startDateTime " +
      "AND sh.submittedAt < :endDateTime " +
      "AND (sh.quiz.id, sh.createdAt) IN (" +
      "  SELECT sh2.quiz.id, MIN(sh2.createdAt) " +
      "  FROM SolveHistory sh2 " +
      "  WHERE sh2.user = :user " +
      "  AND sh2.submittedAt >= :startDateTime " +
      "  AND sh2.submittedAt < :endDateTime " +
      "  GROUP BY sh2.quiz.id" +
      ") " +
      "GROUP BY q.quizType")
  List<QuizTypeSummary> findFirstAttemptsByQuizTypeAndDateTimeRange(
      @Param("user") User user,
      @Param("startDateTime") LocalDateTime startDateTime,
      @Param("endDateTime") LocalDateTime endDateTime
  );

  default List<QuizTypeSummary> findFirstAttemptsByQuizTypeAndDate(User user, LocalDate targetDate) {
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
           SUM(CASE WHEN sh.isCorrect = true THEN 1 ELSE 0 END) as correctCount
    FROM SolveHistory sh
    JOIN sh.quiz q
    WHERE sh.user = :user
      AND sh.submittedAt >= :startDateTime
      AND sh.submittedAt < :endDateTime
      AND (sh.quiz.id, sh.createdAt) IN (
        SELECT sh2.quiz.id, MIN(sh2.createdAt)
        FROM SolveHistory sh2
        WHERE sh2.user = :user
          AND sh2.submittedAt >= :startDateTime
          AND sh2.submittedAt < :endDateTime
        GROUP BY sh2.quiz.id
      )
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

  @Query("SELECT FUNCTION('HOUR', sh.submittedAt) as hourOfDay, " +
      "COUNT(sh) as solvedCount " +
      "FROM SolveHistory sh " +
      "WHERE sh.user = :user " +
      "AND sh.submittedAt >= :startDateTime " +
      "AND sh.submittedAt < :endDateTime " +
      "GROUP BY FUNCTION('HOUR', sh.submittedAt)")
  List<HourlySummary> findHourlySummaryByUserAndDateTimeRange(
      @Param("user") User user,
      @Param("startDateTime") LocalDateTime startDateTime,
      @Param("endDateTime") LocalDateTime endDateTime
  );

  default List<HourlySummary> findHourlySummaryByUserAndDate(User user, LocalDate date) {
    LocalDateTime startDateTime = date.atStartOfDay();
    LocalDateTime endDateTime = date.plusDays(1).atStartOfDay();
    return findHourlySummaryByUserAndDateTimeRange(user, startDateTime, endDateTime);
  }

  interface HourlySummary {
    Integer getHourOfDay();
    Long getSolvedCount();
  }

}
