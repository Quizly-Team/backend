package org.quizly.quizly.core.domain.repository;

import org.quizly.quizly.core.domain.entity.MockExam;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MockExamRepository extends JpaRepository<MockExam, Long> {

}
