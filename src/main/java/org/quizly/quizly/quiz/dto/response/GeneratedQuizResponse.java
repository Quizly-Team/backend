package org.quizly.quizly.quiz.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.quizly.quizly.core.domin.entity.Quiz;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedQuizResponse {

    private String quiz;
    private Quiz.QuizType type;
    private List<String> options;
    private String answer;
    private String explanation;
}
