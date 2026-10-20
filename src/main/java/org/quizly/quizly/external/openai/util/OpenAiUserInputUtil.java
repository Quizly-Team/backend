package org.quizly.quizly.external.openai.util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class OpenAiUserInputUtil {

    private static final String TRUE = "TRUE";
    private static final String FALSE = "FALSE";

    public static List<String> createRandomTrueFalseAnswerList(int quizCount) {
        return IntStream.range(0, quizCount)
            .mapToObj(i -> ThreadLocalRandom.current().nextBoolean() ? TRUE : FALSE)
            .toList();
    }

    public static String createUserInput(String plainText, List<String> answerList) {
        String userInput = "<입력으로 들어온 정리> " + plainText;
        if (answerList == null || answerList.isEmpty()) {
            return userInput;
        }

        String answerOrder = IntStream.range(0, answerList.size())
            .mapToObj(i -> (i + 1) + "번: " + answerList.get(i))
            .collect(Collectors.joining(", "));
        return "<정답 순서> " + answerOrder + "\n" + userInput;
    }
}
