package org.quizly.quizly.core.domin.repository;

import org.quizly.quizly.core.domin.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  RefreshToken findByProviderId(String providerId);
}
