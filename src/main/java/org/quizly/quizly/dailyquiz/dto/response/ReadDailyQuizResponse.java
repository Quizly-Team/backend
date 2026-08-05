package org.quizly.quizly.dailyquiz.dto.response;

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
@Schema(description = "5분 상식 퀴즈 조회 응답")
public class ReadDailyQuizResponse extends BaseResponse<GlobalErrorCode> {

    @Schema(description = "5분 상식 퀴즈 ID", example = "1")
    private Long dailyQuizId;

    @Schema(description = "주제", example = "환경")
    private String topic;

    @Schema(
        description = "문제 워밍업",
        example = "탄소중립의 핵심으로 떠오른 이 기체를 아시나요? 뉴스에서는 자주 접했지만 헷갈렸던 환경 상식, "
            + "Quizly AI가 준비한 3문항의 미니 OX 퀴즈로 가볍게 점검해보세요!"
    )
    private String warmUp;

    @Schema(
        description = "AI가 읽은 원본 자료",
        example = "메탄은 이산화탄소에 이어 두 번째로 큰 온실효과를 일으키는 기체로, ..."
    )
    private String sourceContent;

    @Schema(description = "OX 문항 목록 (문항 번호 오름차순)")
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
