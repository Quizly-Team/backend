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
@Schema(description = "5분 상식 퀴즈 세트 생성 요청")
public class CreateDailyQuizRequest implements BaseRequest {

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

    @Schema(description = "발행 여부. 생략 시 false(미발행)로 생성됩니다.", example = "false")
    private Boolean published;

    @Schema(description = "문항 목록 (최소 1개 이상)")
    private List<QuestionRequest> questions;

    @Override
    public boolean isValid() {
        return topic != null && warmUp != null && sourceContent != null && questions != null;
    }

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
