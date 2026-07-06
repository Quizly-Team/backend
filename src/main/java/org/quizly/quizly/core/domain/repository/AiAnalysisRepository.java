package org.quizly.quizly.core.domain.repository;

import java.util.Optional;
import org.quizly.quizly.core.domain.entity.AiAnalysis;
import org.quizly.quizly.core.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Long> {

    Optional<AiAnalysis> findByUser(User user);
}
