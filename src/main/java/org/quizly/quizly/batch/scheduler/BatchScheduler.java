package org.quizly.quizly.batch.scheduler;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class BatchScheduler {

    private final JobOperator jobOperator;
    private final Job aggregateDailySummaryJob;

    @Scheduled(cron = "0 5 0 * * *")
    public void runAggregateDailySummaryJob() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);

            JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", yesterday.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

            jobOperator.start(aggregateDailySummaryJob, jobParameters);
        } catch (Exception e) {
            log.error("[BatchScheduler] Failed to run aggregateDailySummaryJob", e);
        }
    }
}
