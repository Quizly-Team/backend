package org.quizly.quizly.quiz.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Schema(description = "꼬리 문제 생성 응답")
public class CreateFollowUpQuizResponse extends BaseResponse<GlobalErrorCode> {

    @Schema(description = "꼬리 문제 정보")
    private QuizDetail quiz;

    public record QuizDetail(
        @Schema(description = "문제 ID", example = "11")
        Long quizId,
        @Schema(description = "꼬리 문제의 기반이 된 문제 ID", example = "1")
        Long parentQuizId,
        @Schema(description = "문제", example = "XP의 대규모 릴리즈 원리가 지향하는 방향은?")
        String text,
        @Schema(description = "문제 타입 MULTIPLE_CHOICE:객관식, TRUE_FALSE:OX", example = "MULTIPLE_CHOICE")
        String type,
        @Schema(description = "OX 문제는 빈 배열 반환")
        List<String> options,
        @Schema(description = "정답")
        String answer,
        @Schema(description = "해설")
        String explanation,
        @Schema(description = "주제")
        String topic
    ) {

    }
}
