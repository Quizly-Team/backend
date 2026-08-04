package org.quizly.quizly.core.notification;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class NotificationExecutor {

    public void runQuietly(String context, Runnable task) {
        runQuietly(context, null, task);
    }

    public void runQuietly(String context, Long referenceId, Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            if (referenceId == null) {
                log.warn("[{}] 알림 전송 실패.", context, e);
            } else {
                log.warn("[{}] 알림 전송 실패. referenceId: {}", context, referenceId, e);
            }
        }
    }
}
