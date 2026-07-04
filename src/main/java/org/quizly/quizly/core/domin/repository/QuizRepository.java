package org.quizly.quizly.core.domin.repository;

import org.quizly.quizly.core.domin.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

}
