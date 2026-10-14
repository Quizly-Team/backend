package org.quizly.quizly.core.domain.repository;

import java.time.LocalDate;
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

    @Query(value = """
        SELECT CAST(q.createdAt AS LocalDate) AS groupKey, COUNT(sh) AS quizCount
        FROM SolveHistory sh
        JOIN sh.quiz q
        WHERE sh.user = :user
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
            """)
    Page<DateQuizGroup> findQuizDateGroupsByUser(@Param("user") User user, Pageable pageable);

    @Query(value = """
        SELECT q.topic AS groupKey, COUNT(sh) AS quizCount
        FROM SolveHistory sh
        JOIN sh.quiz q
        WHERE sh.user = :user
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
        WHERE sh.user = :user
        AND CAST(q.createdAt AS LocalDate) = :date
        AND (sh.quiz.id, sh.createdAt) IN (
          SELECT sh2.quiz.id, MAX(sh2.createdAt)
          FROM SolveHistory sh2
          WHERE sh2.user = :user
          GROUP BY sh2.quiz.id
        )
        ORDER BY q.createdAt DESC, q.id DESC
        """,
        countQuery = """
            SELECT COUNT(sh) FROM SolveHistory sh
            JOIN sh.quiz q
            WHERE sh.user = :user
            AND CAST(q.createdAt AS LocalDate) = :date
            AND (sh.quiz.id, sh.createdAt) IN (
              SELECT sh2.quiz.id, MAX(sh2.createdAt)
              FROM SolveHistory sh2
              WHERE sh2.user = :user
              GROUP BY sh2.quiz.id
            )
            """)
    Page<SolveHistory> findLatestSolveHistoriesByUserAndDate(
        @Param("user") User user,
        @Param("date") LocalDate date,
        Pageable pageable
    );

    @Query(value = """
        SELECT sh FROM SolveHistory sh
        JOIN FETCH sh.quiz q
        WHERE sh.user = :user
        AND q.topic = :topic
        AND (sh.quiz.id, sh.createdAt) IN (
          SELECT sh2.quiz.id, MAX(sh2.createdAt)
          FROM SolveHistory sh2
          WHERE sh2.user = :user
          GROUP BY sh2.quiz.id
        )
        ORDER BY q.createdAt DESC, q.id DESC
        """,
        countQuery = """
            SELECT COUNT(sh) FROM SolveHistory sh
            JOIN sh.quiz q
            WHERE sh.user = :user
            AND q.topic = :topic
            AND (sh.quiz.id, sh.createdAt) IN (
              SELECT sh2.quiz.id, MAX(sh2.createdAt)
              FROM SolveHistory sh2
              WHERE sh2.user = :user
              GROUP BY sh2.quiz.id
            )
            """)
    Page<SolveHistory> findLatestSolveHistoriesByUserAndTopic(
        @Param("user") User user,
        @Param("topic") String topic,
        Pageable pageable
    );

    @Query(value = """
        SELECT sh FROM SolveHistory sh
        JOIN FETCH sh.quiz q
        WHERE sh.user = :user
        AND sh.isCorrect = FALSE
        AND CAST(q.createdAt AS LocalDate) = :date
        AND (sh.quiz.id, sh.createdAt) IN (
          SELECT sh2.quiz.id, MAX(sh2.createdAt)
          FROM SolveHistory sh2
          WHERE sh2.user = :user
          GROUP BY sh2.quiz.id
        )
        ORDER BY q.createdAt DESC, q.id DESC
        """,
        countQuery = """
            SELECT COUNT(sh) FROM SolveHistory sh
            JOIN sh.quiz q
            WHERE sh.user = :user
            AND sh.isCorrect = FALSE
            AND CAST(q.createdAt AS LocalDate) = :date
            AND (sh.quiz.id, sh.createdAt) IN (
              SELECT sh2.quiz.id, MAX(sh2.createdAt)
              FROM SolveHistory sh2
              WHERE sh2.user = :user
              GROUP BY sh2.quiz.id
            )
            """)
    Page<SolveHistory> findLatestWrongSolveHistoriesByUserAndDate(
        @Param("user") User user,
        @Param("date") LocalDate date,
        Pageable pageable
    );

    @Query(value = """
        SELECT sh FROM SolveHistory sh
        JOIN FETCH sh.quiz q
        WHERE sh.user = :user
        AND sh.isCorrect = FALSE
        AND q.topic = :topic
        AND (sh.quiz.id, sh.createdAt) IN (
          SELECT sh2.quiz.id, MAX(sh2.createdAt)
          FROM SolveHistory sh2
          WHERE sh2.user = :user
          GROUP BY sh2.quiz.id
        )
        ORDER BY q.createdAt DESC, q.id DESC
        """,
        countQuery = """
            SELECT COUNT(sh) FROM SolveHistory sh
            JOIN sh.quiz q
            WHERE sh.user = :user
            AND sh.isCorrect = FALSE
            AND q.topic = :topic
            AND (sh.quiz.id, sh.createdAt) IN (
              SELECT sh2.quiz.id, MAX(sh2.createdAt)
              FROM SolveHistory sh2
              WHERE sh2.user = :user
              GROUP BY sh2.quiz.id
            )
            """)
    Page<SolveHistory> findLatestWrongSolveHistoriesByUserAndTopic(
        @Param("user") User user,
        @Param("topic") String topic,
        Pageable pageable
    );
}
