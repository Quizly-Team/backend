package org.quizly.quizly.core.domain.repository;

import java.util.Optional;
import org.quizly.quizly.core.domain.entity.DailyQuiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyQuizRepository extends JpaRepository<DailyQuiz, Long> {

    Optional<DailyQuiz> findByIdAndDeletedFalse(Long id);

    Page<DailyQuiz> findAllByDeletedFalse(Pageable pageable);
}
