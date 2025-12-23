package org.quizly.quizly.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.application.BaseRequest;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "일별 통계 집계 요청")
public class BatchAggregateSummaryRequest implements BaseRequest {

  @Schema(description = "집계 대상 날짜", example = "2025-12-01")
  private LocalDate targetDate;

  @Override
  public boolean isValid() {
    return targetDate != null;
  }
}