package org.quizly.quizly.core.domin.repository;

import java.util.List;
import java.util.Optional;
import org.quizly.quizly.core.domin.entity.Faq;
import org.quizly.quizly.core.domin.entity.Faq.FaqCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaqRepository extends JpaRepository<Faq, Long> {

  List<Faq> findAllByDeletedFalseOrderByCreatedAt();

  List<Faq> findByCategoryAndDeletedFalseOrderByCreatedAt(FaqCategory category);

  Optional<Faq> findByIdAndDeletedFalse(Long id);
}
