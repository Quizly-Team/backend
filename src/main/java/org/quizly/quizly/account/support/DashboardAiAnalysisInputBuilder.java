package org.quizly.quizly.account.support;

import org.quizly.quizly.account.service.ReadQuizTypeSummaryService.ReadQuizTypeSummaryResponse.QuizTypeSummary;
import org.quizly.quizly.account.service.ReadTopicSummaryService.ReadTopicSummaryResponse.TopicSummary;
import org.quizly.quizly.account.service.ReadTodaySummaryService.ReadTodaySummaryResponse.TodaySummary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DashboardAiAnalysisInputBuilder {

    public String build(
        TodaySummary today,
        List<QuizTypeSummary> quizTypes,
        List<TopicSummary> topics
    ) {
        int todaySolved = today.solvedCount();
        int todayCorrect = today.correctCount();
        int todayAccuracy =
            todaySolved == 0 ? 0 : (todayCorrect * 100 / todaySolved);

        String quizTypeSummaryText = quizTypes.stream()
            .map(type -> {
                int solved = type.solvedCount();
                int correct = type.correctCount();
                int accuracy =
                    solved == 0 ? 0 : (correct * 100 / solved);

                return "- %s: %d문제, 정답률 %d%%"
                    .formatted(type.quizType(), solved, accuracy);
            })
            .collect(Collectors.joining("\n"));

        String topicSummaryText = topics.stream()
            .map(topic -> {
                int solved = topic.solvedCount();
                int correct = topic.correctCount();
                int accuracy =
                    solved == 0 ? 0 : (correct * 100 / solved);

                return "- %s: %d문제, 정답률 %d%%"
                    .formatted(topic.topic(), solved, accuracy);
            })
            .collect(Collectors.joining("\n"));

        return """
            오늘 풀이 수: %d
            오늘 정답률: %d%%

            문제 유형별 학습 현황:
            %s

            주제별 학습 현황:
            %s
            """.formatted(
            todaySolved,
            todayAccuracy,
            quizTypeSummaryText,
            topicSummaryText
        );
    }
}

