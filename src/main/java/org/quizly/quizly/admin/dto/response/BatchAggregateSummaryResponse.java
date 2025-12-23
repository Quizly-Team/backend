package org.quizly.quizly.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;

@Getter
@SuperBuilder
@NoArgsConstructor
@Schema(description = "일별 통계 집계 응답")
public class BatchAggregateSummaryResponse extends BaseResponse<GlobalErrorCode> {
}