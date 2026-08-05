package org.quizly.quizly.core.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.domain.converter.StringListJsonConverter;
import org.quizly.quizly.core.domain.shared.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "daily_quiz_question")
public class DailyQuizQuestion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_quiz_id", nullable = false)
    private DailyQuiz dailyQuiz;

    @Column(nullable = false)
    private Integer questionNumber;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(nullable = false)
    private String answer;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String explanation;

    @Builder.Default
    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "hashtags", columnDefinition = "JSON", nullable = false)
    private List<String> hashtags = new ArrayList<>();
}
