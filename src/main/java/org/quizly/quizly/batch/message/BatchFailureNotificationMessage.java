package org.quizly.quizly.batch.message;

import org.quizly.quizly.core.notification.NotificationMessage;

public class BatchFailureNotificationMessage implements NotificationMessage {

    private final String jobName;
    private final String reason;

    private final String step;

    private final String parameters;

    public BatchFailureNotificationMessage(String jobName, String reason, String step, String parameters) {
        this.jobName = jobName;
        this.reason = reason;
        this.step = step;
        this.parameters = parameters;
    }

    @Override
    public String title() {
        return "Batch Failure";
    }

    @Override
    public String body() {
        return "job=" + jobName + "\nreason=" + reason + "\nstep=" + step + "\nparameters=" + parameters;
    }
}
