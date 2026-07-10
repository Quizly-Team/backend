package org.quizly.quizly.core.notification;


public interface NotificationMessage {

    String title();

    String body();

    NotificationChannel channel();

    default String threadTs() {
        return null;
    }
}
