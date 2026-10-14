package org.quizly.quizly.core.domain.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.quizly.quizly.core.domain.entity.Quiz;
import org.quizly.quizly.core.domain.entity.SolveHistory;
import org.quizly.quizly.core.domain.entity.User;
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

    interface DateQuizGroup {

        LocalDate getGroupKey();

        Long getQuizCount();
    }

    interface TopicQuizGroup {

        String getGroupKey();

        Long getQuizCount();
    }

    // 풀이자는 항상 문제 소유자이므로(GradeMember*Service) 사용자의 문제에서 출발해 풀이 이력 존재만 확인한다
    @Query(value = """
        SELECT CAST(q.createdAt AS LocalDate) AS groupKey, COUNT(q) AS quizCount
        FROM Quiz q
        WHERE q.user = :user
        AND EXISTS (SELECT 1 FROM SolveHistory sh WHERE sh.user = :user AND sh.quiz = q)
        GROUP BY CAST(q.createdAt AS LocalDate)
        ORDER BY CAST(q.createdAt AS LocalDate) DESC
        """,
        countQuery = """
            SELECT COUNT(DISTINCT CAST(q.createdAt AS LocalDate))
            FROM Quiz q
            WHERE q.user = :user
            AND EXISTS (SELECT 1 FROM SolveHistory sh WHERE sh.user = :user AND sh.quiz = q)
            """)
    Page<DateQuizGroup> findQuizDateGroupsByUser(@Param("user") User user, Pageable pageable);

    @Query(value = """
        SELECT q.topic AS groupKey, COUNT(q) AS quizCount
        FROM Quiz q
        WHERE q.user = :user
        AND EXISTS (SELECT 1 FROM SolveHistory sh WHERE sh.user = :user AND sh.quiz = q)
        GROUP BY q.topic
        ORDER BY q.topic ASC
        """,
        countQuery = """
            SELECT COUNT(DISTINCT q.topic)
            FROM Quiz q
            WHERE q.user = :user
            AND EXISTS (SELECT 1 FROM SolveHistory sh WHERE sh.user = :user AND sh.quiz = q)
            """)
    Page<TopicQuizGroup> findQuizTopicGroupsByUser(@Param("user") User user, Pageable pageable);

    @Query(value = """
        SELECT CAST(q.createdAt AS LocalDate) AS groupKey, COUNT(sh) AS quizCount
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
        GROUP BY CAST(q.createdAt AS LocalDate)
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
    Page<DateQuizGroup> findWrongQuizDateGroupsByUser(@Param("user") User user,
        Pageable pageable);

    @Query(value = """
        SELECT q.topic AS groupKey, COUNT(sh) AS quizCount
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
        GROUP BY q.topic
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
    Page<TopicQuizGroup> findWrongQuizTopicGroupsByUser(@Param("user") User user,
        Pageable pageable);

    @Query(value = """
        SELECT sh FROM SolveHistory sh
        JOIN FETCH sh.quiz q
        WHERE q.user = :user
        AND sh.user = :user
        AND q.createdAt >= :startDateTime AND q.createdAt < :endDateTime
        AND sh.createdAt = (
          SELECT MAX(sh2.createdAt) FROM SolveHistory sh2 WHERE sh2.quiz = q
        )
        ORDER BY q.createdAt DESC, q.id DESC
        """,
        countQuery = """
            SELECT COUNT(q) FROM Quiz q
            WHERE q.user = :user
            AND q.createdAt >= :startDateTime AND q.createdAt < :endDateTime
            AND EXISTS (SELECT 1 FROM SolveHistory sh WHERE sh.user = :user AND sh.quiz = q)
            """)
    Page<SolveHistory> findLatestSolveHistoriesByUserAndDateTimeRange(
        @Param("user") User user,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime,
        Pageable pageable
    );

    @Query(value = """
        SELECT sh FROM SolveHistory sh
        JOIN FETCH sh.quiz q
        WHERE q.user = :user
        AND sh.user = :user
        AND q.topic = :topic
        AND sh.createdAt = (
          SELECT MAX(sh2.createdAt) FROM SolveHistory sh2 WHERE sh2.quiz = q
        )
        ORDER BY q.createdAt DESC, q.id DESC
        """,
        countQuery = """
            SELECT COUNT(q) FROM Quiz q
            WHERE q.user = :user
            AND q.topic = :topic
            AND EXISTS (SELECT 1 FROM SolveHistory sh WHERE sh.user = :user AND sh.quiz = q)
            """)
    Page<SolveHistory> findLatestSolveHistoriesByUserAndTopic(
        @Param("user") User user,
        @Param("topic") String topic,
        Pageable pageable
    );

    @Query(value = """
        SELECT sh FROM SolveHistory sh
        JOIN FETCH sh.quiz q
        WHERE q.user = :user
        AND sh.user = :user
        AND sh.isCorrect = FALSE
        AND q.createdAt >= :startDateTime AND q.createdAt < :endDateTime
        AND sh.createdAt = (
          SELECT MAX(sh2.createdAt) FROM SolveHistory sh2 WHERE sh2.quiz = q
        )
        ORDER BY q.createdAt DESC, q.id DESC
        """,
        countQuery = """
            SELECT COUNT(sh) FROM SolveHistory sh
            JOIN sh.quiz q
            WHERE q.user = :user
            AND sh.user = :user
            AND sh.isCorrect = FALSE
            AND q.createdAt >= :startDateTime AND q.createdAt < :endDateTime
            AND sh.createdAt = (
              SELECT MAX(sh2.createdAt) FROM SolveHistory sh2 WHERE sh2.quiz = q
            )
            """)
    Page<SolveHistory> findLatestWrongSolveHistoriesByUserAndDateTimeRange(
        @Param("user") User user,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime,
        Pageable pageable
    );

    @Query(value = """
        SELECT sh FROM SolveHistory sh
        JOIN FETCH sh.quiz q
        WHERE q.user = :user
        AND sh.user = :user
        AND sh.isCorrect = FALSE
        AND q.topic = :topic
        AND sh.createdAt = (
          SELECT MAX(sh2.createdAt) FROM SolveHistory sh2 WHERE sh2.quiz = q
        )
        ORDER BY q.createdAt DESC, q.id DESC
        """,
        countQuery = """
            SELECT COUNT(sh) FROM SolveHistory sh
            JOIN sh.quiz q
            WHERE q.user = :user
            AND sh.user = :user
            AND sh.isCorrect = FALSE
            AND q.topic = :topic
            AND sh.createdAt = (
              SELECT MAX(sh2.createdAt) FROM SolveHistory sh2 WHERE sh2.quiz = q
            )
            """)
    Page<SolveHistory> findLatestWrongSolveHistoriesByUserAndTopic(
        @Param("user") User user,
        @Param("topic") String topic,
        Pageable pageable
    );

    default Page<SolveHistory> findLatestSolveHistoriesByUserAndDate(User user, LocalDate date,
        Pageable pageable) {
        return findLatestSolveHistoriesByUserAndDateTimeRange(user, date.atStartOfDay(),
            date.plusDays(1).atStartOfDay(), pageable);
    }

    default Page<SolveHistory> findLatestWrongSolveHistoriesByUserAndDate(User user, LocalDate date,
        Pageable pageable) {
        return findLatestWrongSolveHistoriesByUserAndDateTimeRange(user, date.atStartOfDay(),
            date.plusDays(1).atStartOfDay(), pageable);
    }
}
