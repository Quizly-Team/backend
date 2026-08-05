package org.quizly.quizly.admin.support;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import org.quizly.quizly.core.domain.entity.DailyQuizQuestion;
import org.springframework.stereotype.Component;

@Component
public class DailyQuizQuestionValidator {

    public static final int REQUIRED_QUESTION_COUNT = 3;

    private static final String TRUE_ANSWER = "TRUE";
    private static final String FALSE_ANSWER = "FALSE";

    public Result validate(List<DailyQuizQuestionCommand> questions) {
        if (questions == null || questions.stream().anyMatch(
            question -> question == null || !question.isValid())) {
            return Result.violated(Violation.NOT_EXIST_REQUIRED_PARAMETER);
        }

        if (questions.size() != REQUIRED_QUESTION_COUNT) {
            return Result.violated(Violation.INVALID_QUESTION_COUNT);
        }

        if (!hasSequentialQuestionNumbers(questions)) {
            return Result.violated(Violation.INVALID_QUESTION_NUMBER);
        }

        List<DailyQuizQuestion> validatedQuestions = new ArrayList<>();
        for (DailyQuizQuestionCommand question : questions) {
            String normalizedAnswer = normalizeTrueFalseAnswer(question.getAnswer());
            if (normalizedAnswer == null) {
                return Result.violated(Violation.INVALID_TRUE_FALSE_ANSWER);
            }

            validatedQuestions.add(DailyQuizQuestion.builder()
                .questionNumber(question.getQuestionNumber())
                .questionText(question.getQuestionText())
                .answer(normalizedAnswer)
                .explanation(question.getExplanation())
                .build());
        }

        validatedQuestions.sort(
            (left, right) -> Integer.compare(left.getQuestionNumber(), right.getQuestionNumber()));

        return Result.builder().questions(validatedQuestions).build();
    }

    private boolean hasSequentialQuestionNumbers(List<DailyQuizQuestionCommand> questions) {
        Set<Integer> questionNumbers = new HashSet<>();
        for (DailyQuizQuestionCommand question : questions) {
            Integer questionNumber = question.getQuestionNumber();
            if (questionNumber == null || questionNumber < 1
                || questionNumber > REQUIRED_QUESTION_COUNT) {
                return false;
            }
            questionNumbers.add(questionNumber);
        }
        return questionNumbers.size() == REQUIRED_QUESTION_COUNT;
    }

    private String normalizeTrueFalseAnswer(String answer) {
        String normalized = answer.trim().toUpperCase();
        if (normalized.equals("O") || normalized.equals(TRUE_ANSWER)) {
            return TRUE_ANSWER;
        }
        if (normalized.equals("X") || normalized.equals(FALSE_ANSWER)) {
            return FALSE_ANSWER;
        }
        return null;
    }

    public enum Violation {
        NOT_EXIST_REQUIRED_PARAMETER,
        INVALID_QUESTION_COUNT,
        INVALID_QUESTION_NUMBER,
        INVALID_TRUE_FALSE_ANSWER
    }

    @Getter
    @Builder
    public static class Result {

        private Violation violation;
        private List<DailyQuizQuestion> questions;

        private static Result violated(Violation violation) {
            return Result.builder().violation(violation).build();
        }

        public boolean hasViolation() {
            return violation != null;
        }
    }
}
