package org.quizly.quizly.batch.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quizly.quizly.batch.message.BatchFailureNotificationMessage;
import org.quizly.quizly.core.notification.NotificationProvider;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchFailureAlertListener implements JobExecutionListener {

    private final NotificationProvider notificationProvider;

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.FAILED) {

            String jobName = jobExecution.getJobInstance().getJobName();

            String reason = jobExecution.getAllFailureExceptions().isEmpty()
                ? "unknown"
                : jobExecution.getAllFailureExceptions().get(0).getMessage();

            String step = jobExecution.getStepExecutions().stream()
                    .filter(se -> se.getStatus() == BatchStatus.FAILED)
                    .map(se -> se.getStepName() + " (" + se.getExitStatus().getExitCode() + ")")
                    .collect(Collectors.joining(", "));


            String parameters = formatJobParameters(jobExecution);

            notificationProvider.send(
                new BatchFailureNotificationMessage(jobName, reason, step, parameters)
            );
        }
    }

    private String formatJobParameters(JobExecution jobExecution){
        return jobExecution.getJobParameters()
            .getParameters()
            .entrySet()
            .stream()
            .map(e -> e.getKey() + "=" + e.getValue().getValue())
            .collect(Collectors.joining(", "));
    }

}