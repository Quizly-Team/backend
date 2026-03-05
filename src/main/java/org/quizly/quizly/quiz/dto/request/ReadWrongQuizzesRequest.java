package org.quizly.quizly.quiz.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.quizly.quizly.core.presentation.BasePaginationRequest;

@Getter
@Setter
@Schema(description = "틀린 문제 목록 조회 요청")
public class ReadWrongQuizzesRequest extends BasePaginationRequest {

    @Schema(description = "그룹화 기준 (date, topic)", example = "date")
    private String groupType = "date";
}
