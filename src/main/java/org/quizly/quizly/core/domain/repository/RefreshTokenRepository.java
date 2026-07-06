package org.quizly.quizly.core.domain.repository;

import java.util.Optional;
import org.quizly.quizly.core.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
