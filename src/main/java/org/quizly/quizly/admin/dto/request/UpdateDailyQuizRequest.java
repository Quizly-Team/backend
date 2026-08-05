package org.quizly.quizly.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.admin.support.DailyQuizQuestionCommand;
import org.quizly.quizly.core.application.BaseRequest;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "5분 상식 퀴즈 세트 수정 요청. 전달한 필드만 반영됩니다.")
public class UpdateDailyQuizRequest implements BaseRequest {

    @Schema(description = "주제. 생략 시 기존 값 유지", example = "환경")
    private String topic;

    @Schema(description = "문제 워밍업. 생략 시 기존 값 유지", example = "탄소중립의 핵심으로 떠오른 이 기체를 아시나요? ...")
    private String warmUp;

    @Schema(description = "AI가 읽은 원본 자료. 생략 시 기존 값 유지", example = "메탄은 이산화탄소에 이어 ...")
    private String sourceContent;

    @Schema(description = "발행 여부. 생략 시 기존 값 유지", example = "true")
    private Boolean published;

    @Schema(description = "문항 목록. 생략 시 기존 문항 유지, 전달 시 최소 1개 이상으로 전체 교체")
    private List<QuestionRequest> questions;

    public List<DailyQuizQuestionCommand> toQuestionCommands() {
        if (questions == null) {
            return null;
        }

        return questions.stream()
            .map(question -> question == null ? null : DailyQuizQuestionCommand.builder()
                .questionNumber(question.getQuestionNumber())
                .questionText(question.getQuestionText())
                .answer(question.getAnswer())
                .explanation(question.getExplanation())
                .hashtags(question.getHashtags())
                .build())
            .toList();
    }

    @Getter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "5분 상식 퀴즈 OX 문항")
    public static class QuestionRequest {

        @Schema(description = "문항 번호 (1부터 문항 개수까지 중복 없이)", example = "1")
        private Integer questionNumber;

        @Schema(description = "문항", example = "메탄은 이산화탄소보다 온실효과가 크다.")
        private String questionText;

        @Schema(description = "정답. O 또는 X (TRUE/FALSE도 허용)", example = "O")
        private String answer;

        @Schema(description = "해설", example = "메탄의 지구온난화지수는 이산화탄소의 약 28배입니다.")
        private String explanation;

        @Schema(description = "해시태그 목록. 입력한 문자열이 그대로 저장되며 최대 10개까지 저장됩니다.",
            example = "[\"#온실효과\", \"#메테인(CH₄)\", \"##이산화탄소(CO₂)\", \"#기후 완화\"]")
        private List<String> hashtags;
    }
}
