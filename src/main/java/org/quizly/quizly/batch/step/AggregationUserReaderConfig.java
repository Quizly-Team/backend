package org.quizly.quizly.batch.step;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.core.domin.entity.User;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class AggregationUserReaderConfig {

  private final EntityManagerFactory entityManagerFactory;

  @Bean
  @StepScope
  public JpaPagingItemReader<User> aggregationUserReader(
      @Value("#{jobParameters['targetDate']}") String targetDateStr) {

    LocalDate targetDate = LocalDate.parse(targetDateStr);
    LocalDateTime startDateTime = targetDate.atStartOfDay();
    LocalDateTime endDateTime = targetDate.plusDays(1).atStartOfDay();

    JpaPagingItemReader<User> reader = new JpaPagingItemReader<>();
    reader.setName("aggregationUserReader");
    reader.setEntityManagerFactory(entityManagerFactory);
    reader.setQueryString(
        "SELECT u FROM User u " +
        "WHERE EXISTS (" +
        "  SELECT 1 FROM SolveHistory sh " +
        "  WHERE sh.user.id = u.id " +
        "  AND sh.submittedAt >= :startDateTime " +
        "  AND sh.submittedAt < :endDateTime" +
        ") " +
        "ORDER BY u.id ASC"
    );
    reader.setParameterValues(Map.of(
        "startDateTime", startDateTime,
        "endDateTime", endDateTime
    ));
    reader.setPageSize(20);

    return reader;
  }
}