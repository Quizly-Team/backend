package org.quizly.quizly.core.domin.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.quizly.quizly.core.domin.entity.Quiz;
import org.quizly.quizly.core.domin.entity.SolveHistory;
import org.quizly.quizly.core.domin.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolveHistoryRepository extends JpaRepository<SolveHistory, Long> {

  Optional<SolveHistory> findFirstByUserAndQuizOrderByCreatedAtDesc(
      User user,
      Quiz quiz
  );

  @Query(value = """
    SELECT DISTINCT CAST(q.createdAt AS LocalDate)
    FROM SolveHistory sh
    JOIN sh.quiz q
    WHERE sh.user = :user
    AND sh.isCorrect = FALSE
    AND (sh.quiz.id, sh.createdAt) IN (
      SELECT sh2.quiz.id, MAX(sh2.createdAt)
      FROM SolveHistory sh2
      WHERE sh2.user = :user
      GROUP BY sh2.quiz.id
    )
    ORDER BY CAST(q.createdAt AS LocalDate) DESC
    """,
    countQuery = """
    SELECT COUNT(DISTINCT CAST(q.createdAt AS LocalDate))
    FROM SolveHistory sh
    JOIN sh.quiz q
    WHERE sh.user = :user
    AND sh.isCorrect = FALSE
    AND (sh.quiz.id, sh.createdAt) IN (
      SELECT sh2.quiz.id, MAX(sh2.createdAt)
      FROM SolveHistory sh2
      WHERE sh2.user = :user
      GROUP BY sh2.quiz.id
    )
    """)
  Page<LocalDate> findDistinctWrongQuizDatesByUser(@Param("user") User user, Pageable pageable);

  @Query(value = """
    SELECT DISTINCT q.topic
    FROM SolveHistory sh
    JOIN sh.quiz q
    WHERE sh.user = :user
    AND sh.isCorrect = FALSE
    AND (sh.quiz.id, sh.createdAt) IN (
      SELECT sh2.quiz.id, MAX(sh2.createdAt)
      FROM SolveHistory sh2
      WHERE sh2.user = :user
      GROUP BY sh2.quiz.id
    )
    ORDER BY q.topic ASC
    """,
    countQuery = """
    SELECT COUNT(DISTINCT q.topic)
    FROM SolveHistory sh
    JOIN sh.quiz q
    WHERE sh.user = :user
    AND sh.isCorrect = FALSE
    AND (sh.quiz.id, sh.createdAt) IN (
      SELECT sh2.quiz.id, MAX(sh2.createdAt)
      FROM SolveHistory sh2
      WHERE sh2.user = :user
      GROUP BY sh2.quiz.id
    )
    """)
  Page<String> findDistinctWrongQuizTopicsByUser(@Param("user") User user, Pageable pageable);

  @Query("""
    SELECT DISTINCT sh FROM SolveHistory sh
    LEFT JOIN FETCH sh.quiz q
    LEFT JOIN FETCH q.options
    WHERE sh.user = :user
    AND sh.isCorrect = FALSE
    AND CAST(q.createdAt AS LocalDate) IN :dates
    AND (sh.quiz.id, sh.createdAt) IN (
      SELECT sh2.quiz.id, MAX(sh2.createdAt)
      FROM SolveHistory sh2
      WHERE sh2.user = :user
      GROUP BY sh2.quiz.id
    )
    """)
  List<SolveHistory> findLatestWrongSolveHistoriesByUserAndDates(
      @Param("user") User user,
      @Param("dates") List<LocalDate> dates
  );

  @Query("""
    SELECT DISTINCT sh FROM SolveHistory sh
    LEFT JOIN FETCH sh.quiz q
    LEFT JOIN FETCH q.options
    WHERE sh.user = :user
    AND sh.isCorrect = FALSE
    AND q.topic IN :topics
    AND (sh.quiz.id, sh.createdAt) IN (
      SELECT sh2.quiz.id, MAX(sh2.createdAt)
      FROM SolveHistory sh2
      WHERE sh2.user = :user
      GROUP BY sh2.quiz.id
    )
    """)
  List<SolveHistory> findLatestWrongSolveHistoriesByUserAndTopics(
      @Param("user") User user,
      @Param("topics") List<String> topics
  );

  @Query(value = """
    SELECT DISTINCT CAST(q.createdAt AS LocalDate)
    FROM SolveHistory sh
    JOIN sh.quiz q
    WHERE sh.user = :user
    ORDER BY CAST(q.createdAt AS LocalDate) DESC
    """,
      countQuery = """
    SELECT COUNT(DISTINCT CAST(q.createdAt AS LocalDate))
    FROM SolveHistory sh
    JOIN sh.quiz q
    WHERE sh.user = :user
    """)
  Page<LocalDate> findDistinctQuizDatesByUser(@Param("user") User user, Pageable pageable);

  @Query(value = """
    SELECT DISTINCT q.topic
    FROM SolveHistory sh
    JOIN sh.quiz q
    WHERE sh.user = :user
    ORDER BY q.topic ASC
    """,
      countQuery = """
    SELECT COUNT(DISTINCT q.topic)
    FROM SolveHistory sh
    JOIN sh.quiz q
    WHERE sh.user = :user
    """)
  Page<String> findDistinctQuizTopicsByUser(@Param("user") User user, Pageable pageable);

  @Query("""
    SELECT DISTINCT sh FROM SolveHistory sh
    LEFT JOIN FETCH sh.quiz q
    LEFT JOIN FETCH q.options
    WHERE sh.user = :user
    AND CAST(q.createdAt AS LocalDate) IN :dates
    AND (sh.quiz.id, sh.createdAt) IN (
      SELECT sh2.quiz.id, MAX(sh2.createdAt)
      FROM SolveHistory sh2
      WHERE sh2.user = :user
      GROUP BY sh2.quiz.id
    )
    """)
  List<SolveHistory> findLatestSolveHistoriesByUserAndDates(
      @Param("user") User user,
      @Param("dates") List<LocalDate> dates
  );

  @Query("""
    SELECT DISTINCT sh FROM SolveHistory sh
    LEFT JOIN FETCH sh.quiz q
    LEFT JOIN FETCH q.options
    WHERE sh.user = :user
    AND q.topic IN :topics
    AND (sh.quiz.id, sh.createdAt) IN (
      SELECT sh2.quiz.id, MAX(sh2.createdAt)
      FROM SolveHistory sh2
      WHERE sh2.user = :user
      GROUP BY sh2.quiz.id
    )
    """)
  List<SolveHistory> findLatestSolveHistoriesByUserAndTopics(
      @Param("user") User user,
      @Param("topics") List<String> topics
  );
}
