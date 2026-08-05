package org.quizly.quizly.core.domain.repository;

import java.util.List;
import java.util.Optional;
import org.quizly.quizly.core.domain.entity.DailyQuiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DailyQuizRepository extends JpaRepository<DailyQuiz, Long> {

    Optional<DailyQuiz> findByIdAndDeletedFalse(Long id);

    Page<DailyQuiz> findAllByDeletedFalse(Pageable pageable);
    
    @Query("SELECT d.id FROM DailyQuiz d WHERE d.published = true AND d.deleted = false")
    List<Long> findAllPublishedIds();
}
