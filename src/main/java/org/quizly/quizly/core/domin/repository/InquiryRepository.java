package org.quizly.quizly.core.domin.repository;

import org.quizly.quizly.core.domin.entity.Inquiry;
import org.quizly.quizly.core.domin.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry,Long> {
    List<Inquiry> findAllByUser(User user);
    Optional<Inquiry> findById(Long id);
}
