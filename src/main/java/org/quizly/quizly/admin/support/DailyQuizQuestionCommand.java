package org.quizly.quizly.admin.support;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DailyQuizQuestionCommand {

    private Integer questionNumber;
    private String questionText;
    private String answer;
    private String explanation;

    public boolean isValid() {
        return questionText != null && !questionText.isBlank()
            && answer != null && !answer.isBlank()
            && explanation != null && !explanation.isBlank();
    }
}
