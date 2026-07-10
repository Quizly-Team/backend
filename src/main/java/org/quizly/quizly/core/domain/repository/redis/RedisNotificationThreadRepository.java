package org.quizly.quizly.core.domain.repository.redis;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.core.notification.NotificationThreadRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisNotificationThreadRepository implements NotificationThreadRepository {

    private static final String KEY_PREFIX = "slack:signup:thread:";
    private static final long TTL_DAYS = 7;

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(Long referenceId, String threadTs) {
        redisTemplate.opsForValue().set(key(referenceId), threadTs, TTL_DAYS, TimeUnit.DAYS);
    }

    @Override
    public Optional<String> find(Long referenceId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(referenceId)));
    }

    @Override
    public void delete(Long referenceId) {
        redisTemplate.delete(key(referenceId));
    }

    private String key(Long referenceId) {
        return KEY_PREFIX + referenceId;
    }
}
