package org.quizly.quizly.batch.step;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.entity.User.Provider;
import org.quizly.quizly.core.domin.entity.User.Role;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class AggregationUserReaderConfig {

  private final DataSource dataSource;

  @Bean
  @StepScope
  public JdbcPagingItemReader<User> aggregationUserReader(
      @Value("#{jobParameters['targetDate']}") String targetDateStr) {

    LocalDate targetDate = LocalDate.parse(targetDateStr);
    LocalDateTime startDateTime = targetDate.atStartOfDay();
    LocalDateTime endDateTime = targetDate.plusDays(1).atStartOfDay();

    MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
    queryProvider.setSelectClause("SELECT u.*");
    queryProvider.setFromClause("FROM user u");
    queryProvider.setWhereClause(
        "WHERE EXISTS (" +
        "  SELECT 1 FROM solve_history sh USE INDEX (idx_solve_history_submitted_user)" +
        "  WHERE sh.user_id = u.id" +
        "  AND sh.submitted_at >= :startDateTime" +
        "  AND sh.submitted_at < :endDateTime" +
        ")"
    );
    queryProvider.setSortKeys(Map.of("id", Order.ASCENDING));

    JdbcPagingItemReader<User> reader = new JdbcPagingItemReader<>();
    reader.setName("aggregationUserReader");
    reader.setDataSource(dataSource);
    reader.setQueryProvider(queryProvider);
    reader.setParameterValues(Map.of(
        "startDateTime", startDateTime,
        "endDateTime", endDateTime
    ));
    reader.setPageSize(20);
    reader.setRowMapper((rs, rowNum) -> {
      String provider = rs.getString("provider");

      return User.builder()
          .id(rs.getLong("id"))
          .name(rs.getString("name"))
          .nickName(rs.getString("nick_name"))
          .email(rs.getString("email"))
          .role(Role.valueOf(rs.getString("role")))
          .profileImageUrl(rs.getString("profile_image_url"))
          .targetType(rs.getString("target_type"))
          .studyGoal(rs.getString("study_goal"))
          .onboardingCompleted(rs.getBoolean("onboarding_completed"))
          .createdAt(rs.getTimestamp("created_at") != null
              ? rs.getTimestamp("created_at").toLocalDateTime() : null)
          .updatedAt(rs.getTimestamp("updated_at") != null
              ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
          .deleted(rs.getBoolean("deleted"))
          .provider(provider != null ? Provider.valueOf(provider) : null)
          .providerId(rs.getString("provider_id"))
          .build();
    });

    return reader;
  }
}