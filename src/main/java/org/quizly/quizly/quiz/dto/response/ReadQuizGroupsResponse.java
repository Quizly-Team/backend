package org.quizly.quizly.quiz.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.core.presentation.Pagination;

@Getter
@NoArgsConstructor
@ToString
@SuperBuilder
@Schema(description = "문제 그룹 목록 조회 응답")
public class ReadQuizGroupsResponse extends BaseResponse<GlobalErrorCode> {

    @Schema(description = "문제 그룹 목록")
    private List<QuizGroup> quizGroupList;

    @Schema(description = "페이지네이션 정보 (그룹 수 기준)")
    private Pagination pagination;

    @Schema(description = "문제 그룹 정보")
    public record QuizGroup(
        @Schema(description = "그룹명 (date: yyyy-MM-dd, topic: 주제명)", example = "자바")
        String group,
        @Schema(description = "그룹에 속한 문제 수", example = "12")
        long quizCount
    ) {

    }
}
