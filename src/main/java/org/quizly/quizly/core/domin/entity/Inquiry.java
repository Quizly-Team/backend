package org.quizly.quizly.core.domin.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.domin.shared.BaseEntity;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "inquiry")
public class Inquiry extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;
    @Getter
    @RequiredArgsConstructor
    public enum Status{
        WAITING("답변 대기중"),
        COMPLETED("답변 완료");

        private final String description;
    }
    @Column(nullable = true)
    private String reply;

    @Column(nullable = true)
    private LocalDateTime repliedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

}
