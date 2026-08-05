package org.quizly.quizly.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;

@Getter
@SuperBuilder
@NoArgsConstructor
@Schema(description = "5분 상식 퀴즈 세트 수정 응답")
public class UpdateDailyQuizResponse extends BaseResponse<GlobalErrorCode> {

    @Schema(description = "5분 상식 퀴즈 세트 ID", example = "1")
    private Long dailyQuizId;

    @Schema(description = "주제", example = "환경")
    private String topic;

    @Schema(description = "발행 여부", example = "true")
    private Boolean published;

    @Schema(description = "문제 워밍업", example = "탄소중립의 핵심으로 떠오른 이 기체를 아시나요? ...")
    private String warmUp;

    @Schema(description = "AI가 읽은 원본 자료", example = "메탄은 이산화탄소에 이어 ...")
    private String sourceContent;

    @Schema(description = "문항 목록")
    private List<QuestionDetail> questions;

    public record QuestionDetail(
        @Schema(description = "문항 ID", example = "1") Long questionId,
        @Schema(description = "문항 번호", example = "1") Integer questionNumber,
        @Schema(description = "문항", example = "메탄은 이산화탄소보다 온실효과가 크다.") String questionText,
        @Schema(description = "정답. TRUE 또는 FALSE", example = "TRUE") String answer,
        @Schema(description = "해설", example = "메탄의 지구온난화지수는 이산화탄소의 약 28배입니다.")
        String explanation,
        @Schema(description = "해시태그 목록 (최대 10개)", example = "[\"#기후\", \"#날씨\", \"#온난화\"]")
        List<String> hashtags
    ) {

    }
}
