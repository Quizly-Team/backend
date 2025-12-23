package org.quizly.quizly.batch.step;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.entity.UserQuizTypeDailySummary;
import org.quizly.quizly.core.domin.repository.SolveHistoryRepository;
import org.quizly.quizly.core.domin.repository.SolveHistoryRepository.QuizTypeSummary;
import org.quizly.quizly.core.domin.repository.UserQuizTypeDailySummaryRepository;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Log4j2
public class AggregateUserQuizTypeDailySummaryStepConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final SolveHistoryRepository solveHistoryRepository;
  private final UserQuizTypeDailySummaryRepository quizTypeDailySummaryRepository;

  @Bean
  public Step aggregateUserQuizTypeDailySummaryStep(
      ItemReader<User> aggregationUserReader,
      ItemProcessor<User, List<UserQuizTypeDailySummary>> aggregateUserQuizTypeDailySummaryProcessor,
      ItemWriter<List<UserQuizTypeDailySummary>> aggregateUserQuizTypeDailySummaryWriter) {
    return new StepBuilder("aggregateUserQuizTypeDailySummaryStep", jobRepository)
        .<User, List<UserQuizTypeDailySummary>>chunk(20, transactionManager)
        .reader(aggregationUserReader)
        .processor(aggregateUserQuizTypeDailySummaryProcessor)
        .writer(aggregateUserQuizTypeDailySummaryWriter)
        .build();
  }

  @Bean
  @StepScope
  public ItemProcessor<User, List<UserQuizTypeDailySummary>> aggregateUserQuizTypeDailySummaryProcessor(
      @Value("#{jobParameters['targetDate']}") String targetDateStr) {
    return user -> {
      try {
        LocalDate targetDate = LocalDate.parse(targetDateStr);

        List<QuizTypeSummary> typeSummaryList =
            solveHistoryRepository.findFirstAttemptsByQuizTypeAndDate(user, targetDate);

        List<UserQuizTypeDailySummary> summaryList = new ArrayList<>();
        for (QuizTypeSummary typeSummary : typeSummaryList) {
          UserQuizTypeDailySummary summary = quizTypeDailySummaryRepository
              .findByUserAndQuizTypeAndDate(user, typeSummary.getQuizType(), targetDate)
              .orElse(UserQuizTypeDailySummary.builder()
                  .user(user)
                  .quizType(typeSummary.getQuizType())
                  .date(targetDate)
                  .build());

          summary.setSolvedCount(typeSummary.getTotalCount() != null ? typeSummary.getTotalCount().intValue() : 0);
          summary.setCorrectCount(typeSummary.getCorrectCount() != null ? typeSummary.getCorrectCount().intValue() : 0);

          summaryList.add(summary);
        }

        return summaryList;
      } catch (Exception e) {
        log.error("[AggregateUserQuizTypeDailySummaryProcessor] Failed for user: {}, targetDate: {}",
            user.getId(), targetDateStr, e);
        return null;
      }
    };
  }

  @Bean
  @StepScope
  public ItemWriter<List<UserQuizTypeDailySummary>> aggregateUserQuizTypeDailySummaryWriter() {
    return items -> {
      for (List<UserQuizTypeDailySummary> summaryList : items) {
        if (summaryList != null && !summaryList.isEmpty()) {
          quizTypeDailySummaryRepository.saveAll(summaryList);
        }
      }
    };
  }
}