package org.quizly.quizly.core.domin.repository;

import java.util.List;
import org.quizly.quizly.core.domin.entity.Quiz;
import org.quizly.quizly.core.domin.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
  List<Quiz> findAllByUser(User user);

  @Query("SELECT DISTINCT q FROM Quiz q LEFT JOIN FETCH q.options WHERE q.user = :user")
  List<Quiz> findAllByUserWithOptions(User user);
}
