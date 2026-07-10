package org.quizly.quizly.core.notification;

import java.util.Optional;

public interface NotificationThreadRepository {

    void save(Long referenceId, String threadTs);

    Optional<String> find(Long referenceId);

    void delete(Long referenceId);
}
