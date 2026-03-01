package org.quizly.quizly.core.domin.repository;

import org.quizly.quizly.core.domin.entity.Inquiry;
import org.quizly.quizly.core.domin.entity.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry,Long> {
    List<Inquiry> findAllByUser(User user);
    @Query("SELECT i FROM Inquiry i JOIN FETCH i.user ")
    List<Inquiry> findAllWithUser(Sort sort);

    @Query("SELECT i FROM Inquiry i JOIN FETCH i.user WHERE i.status =:status")
    List<Inquiry> findAllByStatusWithUser(@Param("status") Inquiry.Status status, Sort sort);
}
