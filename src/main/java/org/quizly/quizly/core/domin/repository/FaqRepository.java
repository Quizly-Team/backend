package org.quizly.quizly.core.domin.repository;

import org.quizly.quizly.core.domin.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaqRepository extends JpaRepository<Faq, Long> {
}
