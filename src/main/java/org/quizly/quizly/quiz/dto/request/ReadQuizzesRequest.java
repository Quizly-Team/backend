package org.quizly.quizly.quiz.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.quizly.quizly.core.presentation.BasePaginationRequest;

@Getter
@Setter
@Schema(description = "그룹별 문제 목록 조회 요청")
public class ReadQuizzesRequest extends BasePaginationRequest {

    private static final int DEFAULT_QUIZ_PAGE_SIZE = 20;

    @Schema(description = "그룹화 기준 (date, topic)", example = "date")
    private String groupType = "date";

    @Schema(description = "그룹 (date: yyyy-MM-dd, topic: 주제명)", example = "2026-10-07")
    private String group;

    public ReadQuizzesRequest() {
        this.pageSize = DEFAULT_QUIZ_PAGE_SIZE;
    }
}
