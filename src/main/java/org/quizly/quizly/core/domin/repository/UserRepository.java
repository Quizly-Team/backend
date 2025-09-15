package org.quizly.quizly.core.domin.repository;

import org.quizly.quizly.core.domin.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  User findByProviderId(String providerId);
}
