package org.quizly.quizly.batch.step;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.entity.SolveHourlySummary;
import org.quizly.quizly.core.domin.repository.SolveHistoryRepository;
import org.quizly.quizly.core.domin.repository.SolveHistoryRepository.HourlySummary;
import org.quizly.quizly.core.domin.repository.SolveHourlySummaryRepository;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
@Log4j2
public class AggregateSolveHourlySummaryStepConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final SolveHistoryRepository solveHistoryRepository;
  private final SolveHourlySummaryRepository solveHourlySummaryRepository;

  @Bean
  public Step aggregateSolveHourlySummaryStep(
      ItemReader<User> aggregationUserReader,
      ItemProcessor<User, List<SolveHourlySummary>> aggregateSolveHourlySummaryProcessor,
      ItemWriter<List<SolveHourlySummary>> aggregateSolveHourlySummaryWriter) {
    return new StepBuilder("aggregateSolveHourlySummaryStep", jobRepository)
        .<User, List<SolveHourlySummary>>chunk(20, transactionManager)
        .reader(aggregationUserReader)
        .processor(aggregateSolveHourlySummaryProcessor)
        .writer(aggregateSolveHourlySummaryWriter)
        .build();
  }

  @Bean
  @StepScope
  public ItemProcessor<User, List<SolveHourlySummary>> aggregateSolveHourlySummaryProcessor(
      @Value("#{jobParameters['targetDate']}") String targetDateStr) {
    return user -> {
      try {
        LocalDate targetDate = LocalDate.parse(targetDateStr);

        List<HourlySummary> hourlySummaryList =
            solveHistoryRepository.findHourlySummaryByUserAndDate(user, targetDate);

        List<SolveHourlySummary> existSolveHourlySummaryList =
            solveHourlySummaryRepository.findByUserAndDate(user, targetDate);

        Map<Integer, SolveHourlySummary> existingMap = existSolveHourlySummaryList.stream()
            .collect(Collectors.toMap(SolveHourlySummary::getHour, Function.identity()));

        List<SolveHourlySummary> solveHourlySummaryList = new ArrayList<>();
        for (HourlySummary hourlySummary : hourlySummaryList) {
          Integer hour = hourlySummary.getHourOfDay();
          if (hour == null) {
            continue;
          }

          SolveHourlySummary solveHourlySummary = existingMap.getOrDefault(hour,
              SolveHourlySummary.builder()
                  .user(user)
                  .date(targetDate)
                  .hour(hour)
                  .build()
          );

          solveHourlySummary.setSolvedCount(
              hourlySummary.getSolvedCount() != null
                  ? hourlySummary.getSolvedCount().intValue()
                  : 0
          );

          solveHourlySummaryList.add(solveHourlySummary);
        }

        return solveHourlySummaryList;
      } catch (Exception e) {
        log.error("[AggregateSolveHourlySummaryProcessor] Failed for user: {}, targetDate: {}",
            user.getId(), targetDateStr, e);
        return null;
      }
    };
  }

  @Bean
  @StepScope
  public ItemWriter<List<SolveHourlySummary>> aggregateSolveHourlySummaryWriter() {
    return items -> {
      List<SolveHourlySummary> solveHourlySummaryList = new ArrayList<>();
      for (List<SolveHourlySummary> solveHourlySummary : items) {
        if (solveHourlySummary != null && !solveHourlySummary.isEmpty()) {
          solveHourlySummaryList.addAll(solveHourlySummary);
        }
      }

      if (!solveHourlySummaryList.isEmpty()) {
        solveHourlySummaryRepository.saveAll(solveHourlySummaryList);
      }
    };
  }
}
