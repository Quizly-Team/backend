package org.quizly.quizly.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Log4j2
public class BatchScheduler {

  private final JobLauncher jobLauncher;
  private final Job aggregateDailySummaryJob;

  @Scheduled(cron = "0 5 0 * * *")
  public void runAggregateDailySummaryJob() {
    try {
      LocalDate yesterday = LocalDate.now().minusDays(1);

      JobParameters jobParameters = new JobParametersBuilder()
          .addString("targetDate", yesterday.toString())
          .addLong("timestamp", System.currentTimeMillis())
          .toJobParameters();

      jobLauncher.run(aggregateDailySummaryJob, jobParameters);
    } catch (Exception e) {
      log.error("[BatchScheduler] Failed to run aggregateDailySummaryJob", e);
    }
  }
}
