package org.quizly.quizly.core.domin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.domin.shared.BaseEntity;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "ai_analysis")
public class AiAnalysis extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate analysisDate;

    @Column(name = "analyzed_quiz_count", nullable = false)
    private int analyzedSolvedCount;

    @Column(name = "analysis_result", columnDefinition = "TEXT")
    private String analysisResult;

    public boolean isDirty(int currentSolvedCount) {
        return currentSolvedCount > analyzedSolvedCount;
    }

    public void updateAfterAnalysis(int currentSolvedCount, String result) {
        this.analyzedSolvedCount = currentSolvedCount;
        this.analysisResult = result;
    }

}


