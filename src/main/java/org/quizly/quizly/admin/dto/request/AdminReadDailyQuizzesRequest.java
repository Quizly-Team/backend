package org.quizly.quizly.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.quizly.quizly.core.presentation.BasePaginationRequest;

@Getter
@Setter
@Schema(description = "5분 상식 퀴즈 세트 목록 조회 요청")
public class AdminReadDailyQuizzesRequest extends BasePaginationRequest {

}
