package org.quizly.quizly.admin.support;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DailyQuizQuestionCommand {

    private Integer questionNumber;
    private String questionText;
    private String answer;
    private String explanation;
    private List<String> hashtags;

    public boolean isValid() {
        return questionText != null && !questionText.isBlank()
            && answer != null && !answer.isBlank()
            && explanation != null && !explanation.isBlank();
    }
}
