package org.quizly.quizly.batch.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Log4j2
public class AggregateDailySummaryJobConfig {

  private final JobRepository jobRepository;

  @Bean
  public Job aggregateDailySummaryJob(
      Step aggregateUserQuizTypeDailySummaryStep) {
    return new JobBuilder("aggregateDailySummaryJob", jobRepository)
        .start(aggregateUserQuizTypeDailySummaryStep)
        .build();
  }
}
