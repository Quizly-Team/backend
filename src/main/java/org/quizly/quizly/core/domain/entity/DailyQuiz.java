package org.quizly.quizly.core.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.domain.shared.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "daily_quiz")
public class DailyQuiz extends BaseEntity {

    @Column(nullable = false)
    private String topic;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String warmUp;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String sourceContent;

    @Builder.Default
    @Column(nullable = false)
    private Boolean published = false;

    @Builder.Default
    @OneToMany(mappedBy = "dailyQuiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionNumber ASC")
    private List<DailyQuizQuestion> questions = new ArrayList<>();

    public void replaceQuestions(List<DailyQuizQuestion> newQuestions) {
        questions.clear();
        newQuestions.forEach(question -> question.setDailyQuiz(this));
        questions.addAll(newQuestions);
    }

    public void update(String topic, String warmUp, String sourceContent, Boolean published) {
        if (topic != null) {
            this.topic = topic;
        }
        if (warmUp != null) {
            this.warmUp = warmUp;
        }
        if (sourceContent != null) {
            this.sourceContent = sourceContent;
        }
        if (published != null) {
            this.published = published;
        }
    }

    public void softDelete() {
        setDeleted(true);
    }
}
