package org.quizly.quizly.core.domin.repository;

import java.util.List;
import org.quizly.quizly.core.domin.entity.SolveHistory;
import org.quizly.quizly.core.domin.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolveHistoryRepository extends JpaRepository<SolveHistory, Long> {

  @Query("SELECT sh FROM SolveHistory sh LEFT JOIN FETCH sh.quiz WHERE sh.user = :user AND (sh.quiz.id, sh.createdAt) IN (SELECT sh2.quiz.id, MAX(sh2.createdAt) FROM SolveHistory sh2 WHERE sh2.user = :user GROUP BY sh2.quiz.id)")
  List<SolveHistory> findLatestSolveHistoriesByUser(@Param("user") User user);

  @Query("SELECT sh FROM SolveHistory sh LEFT JOIN FETCH sh.quiz WHERE sh.user = :user AND sh.isCorrect = FALSE AND (sh.quiz.id, sh.createdAt) IN (SELECT sh2.quiz.id, MAX(sh2.createdAt) FROM SolveHistory sh2 WHERE sh2.user = :user GROUP BY sh2.quiz.id)")
  List<SolveHistory> findLatestWrongSolveHistoriesByUser(@Param("user") User user);
}
