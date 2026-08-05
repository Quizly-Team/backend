package org.quizly.quizly.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.core.presentation.Pagination;

@Getter
@SuperBuilder
@NoArgsConstructor
@Schema(description = "5분 상식 퀴즈 세트 목록 조회 응답")
public class AdminReadDailyQuizzesResponse extends BaseResponse<GlobalErrorCode> {

    @Schema(description = "세트 목록 (최신순)")
    private List<AdminDailyQuizDetail> dailyQuizList;

    @Schema(description = "페이지네이션")
    private Pagination pagination;

    public record AdminDailyQuizDetail(
        @Schema(description = "5분 상식 퀴즈 세트 ID", example = "1") Long dailyQuizId,
        @Schema(description = "주제", example = "환경") String topic,
        @Schema(description = "발행 여부", example = "true") Boolean published,
        @Schema(description = "문제 워밍업", example = "탄소중립의 핵심으로 떠오른 이 기체를 아시나요? ...")
        String warmUp,
        @Schema(description = "생성일시", example = "2026-08-05T17:43:13") LocalDateTime createdAt,
        @Schema(description = "수정일시", example = "2026-08-05T18:02:41") LocalDateTime updatedAt
    ) {

    }
}
