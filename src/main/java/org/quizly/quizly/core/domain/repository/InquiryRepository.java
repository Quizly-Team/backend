package org.quizly.quizly.core.domain.repository;

import java.util.List;
import org.quizly.quizly.core.domain.entity.Inquiry;
import org.quizly.quizly.core.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findAllByUser(User user);

    @Query(value = "SELECT i FROM Inquiry i JOIN FETCH i.user",
        countQuery = "SELECT count(i) FROM Inquiry i")
    Page<Inquiry> findAllWithUser(Pageable pageable);

    @Query(value = "SELECT i FROM Inquiry i JOIN FETCH i.user WHERE i.status = :status",
        countQuery = "SELECT count(i) FROM Inquiry i WHERE i.status = :status")
    Page<Inquiry> findAllByStatusWithUser(@Param("status") Inquiry.Status status,
        Pageable pageable);
}
