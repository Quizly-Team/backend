package org.quizly.quizly.core.domain.repository;

import java.util.Optional;
import org.quizly.quizly.core.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderId(String providerId);
}
