package org.quizly.quizly.core.domin.repository;

import org.quizly.quizly.core.domin.entity.AiAnalysis;
import org.quizly.quizly.core.domin.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis,Long> {
    Optional<AiAnalysis> findByUser(User user);
}
