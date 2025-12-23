package org.quizly.quizly.core.domin.repository;

import org.quizly.quizly.core.domin.entity.Quiz.QuizType;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.entity.UserQuizTypeDailySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserQuizTypeDailySummaryRepository extends JpaRepository<UserQuizTypeDailySummary, Long> {

  Optional<UserQuizTypeDailySummary> findByUserAndQuizTypeAndDate(
      User user,
      QuizType quizType,
      LocalDate date
  );
}