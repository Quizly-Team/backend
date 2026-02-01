package org.quizly.quizly.core.domin.repository;

import java.util.Optional;
import org.quizly.quizly.core.domin.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByProviderId(String providerId);

  void deleteByProviderId(String providerId);
}
