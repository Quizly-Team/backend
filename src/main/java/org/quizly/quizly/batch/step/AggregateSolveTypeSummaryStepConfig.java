package org.quizly.quizly.batch.step;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.entity.SolveTypeSummary;
import org.quizly.quizly.core.domin.repository.SolveHistoryRepository;
import org.quizly.quizly.core.domin.repository.SolveHistoryRepository.QuizTypeSummary;
import org.quizly.quizly.core.domin.repository.SolveTypeSummaryRepository;
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
public class AggregateSolveTypeSummaryStepConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final SolveHistoryRepository solveHistoryRepository;
  private final SolveTypeSummaryRepository solveTypeSummaryRepository;

  @Bean
  public Step aggregateSolveTypeSummaryStep(
      ItemReader<User> aggregationUserReader,
      ItemProcessor<User, List<SolveTypeSummary>> aggregateSolveTypeSummaryProcessor,
      ItemWriter<List<SolveTypeSummary>> aggregateSolveTypeSummaryWriter) {
    return new StepBuilder("aggregateSolveTypeSummaryStep", jobRepository)
        .<User, List<SolveTypeSummary>>chunk(20, transactionManager)
        .reader(aggregationUserReader)
        .processor(aggregateSolveTypeSummaryProcessor)
        .writer(aggregateSolveTypeSummaryWriter)
        .build();
  }

  @Bean
  @StepScope
  public ItemProcessor<User, List<SolveTypeSummary>> aggregateSolveTypeSummaryProcessor(
      @Value("#{jobParameters['targetDate']}") String targetDateStr) {
    return user -> {
      try {
        LocalDate targetDate = LocalDate.parse(targetDateStr);

        List<QuizTypeSummary> typeSummaryList =
            solveHistoryRepository.findFirstAttemptsByQuizTypeAndDate(user, targetDate);

        List<SolveTypeSummary> solveTypeSummaryList = new ArrayList<>();
        for (QuizTypeSummary typeSummary : typeSummaryList) {
          SolveTypeSummary solveTypeSummary = solveTypeSummaryRepository
              .findByUserAndQuizTypeAndDate(user, typeSummary.getQuizType(), targetDate)
              .orElse(SolveTypeSummary.builder()
                  .user(user)
                  .quizType(typeSummary.getQuizType())
                  .date(targetDate)
                  .build());

          solveTypeSummary.setSolvedCount(
              Optional.ofNullable(typeSummary.getTotalCount()).map(Long::intValue).orElse(0));
          solveTypeSummary.setCorrectCount(
              Optional.ofNullable(typeSummary.getCorrectCount()).map(Long::intValue).orElse(0));

          solveTypeSummaryList.add(solveTypeSummary);
        }

        return solveTypeSummaryList;
      } catch (Exception e) {
        log.error("[AggregateSolveTypeSummaryProcessor] Failed for user: {}, targetDate: {}",
            user.getId(), targetDateStr, e);
        return null;
      }
    };
  }

  @Bean
  @StepScope
  public ItemWriter<List<SolveTypeSummary>> aggregateSolveTypeSummaryWriter() {
    return items -> {
      for (List<SolveTypeSummary> solveTypeSummaryList : items) {
        if (solveTypeSummaryList != null && !solveTypeSummaryList.isEmpty()) {
          solveTypeSummaryRepository.saveAll(solveTypeSummaryList);
        }
      }
    };
  }
}