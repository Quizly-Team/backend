package org.quizly.quizly.core.domain.repository;

import org.quizly.quizly.core.domain.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

}
