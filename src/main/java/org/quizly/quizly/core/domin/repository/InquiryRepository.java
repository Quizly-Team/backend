package org.quizly.quizly.core.domin.repository;

import org.quizly.quizly.core.domin.entity.Inquiry;
import org.quizly.quizly.core.domin.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry,Long> {
    List<Inquiry> findAllByUser(User user);
    @Query(value = "SELECT i FROM Inquiry i JOIN FETCH i.user",
        countQuery = "SELECT count(i) FROM Inquiry i")
    Page<Inquiry> findAllWithUser(Pageable pageable);

    @Query(value = "SELECT i FROM Inquiry i JOIN FETCH i.user WHERE i.status = :status",
        countQuery = "SELECT count(i) FROM Inquiry i WHERE i.status = :status")
    Page<Inquiry> findAllByStatusWithUser(@Param("status") Inquiry.Status status, Pageable pageable);
}
