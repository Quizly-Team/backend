package org.quizly.quizly.mock.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.quizly.quizly.core.application.BaseRequest;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "모의고사 생성 요청")
public class CreateMemberMockExamRequest implements BaseRequest {

  @Getter
  @RequiredArgsConstructor
  public enum MockExamType {
    FIND_CORRECT(TypeCategory.SELECTION, "prompt/mock_exam/find_correct.txt"),
    FIND_INCORRECT(TypeCategory.SELECTION, "prompt/mock_exam/find_incorrect.txt"),
    FIND_MATCH(TypeCategory.SELECTION, "prompt/mock_exam/find_match.txt"),
    ESSAY(TypeCategory.DESCRIPTIVE, "prompt/mock_exam/essay.txt"),
    SHORT_ANSWER(TypeCategory.DESCRIPTIVE, "prompt/mock_exam/short_answer.txt"),
    TRUE_FALSE(TypeCategory.DESCRIPTIVE, "prompt/mock_exam/true_false.txt");

    private final TypeCategory typeCategory;
    private final String promptPath;

    public enum TypeCategory { DESCRIPTIVE, SELECTION }
  }

  @Schema(description = "사용자 정리", example = "개발 방법론에는 Agile이 있으며, 대표적인 프로세스로는 XP와 Scrum이 있다.\n\nXP는 계획 절차, 소규모 릴리즈, 상징(Metaphor), 공동 소유, 지속적인 통합(CI)을 기본 원리로 한다.\n\n럼바우 방법론은 객체 모델링, 동적 모델링, 기능 모델링으로 구성되며 각각 객체 다이어그램, 상태 다이어그램, 자료 흐름도로 표현된다.")
  private String plainText;

  @Schema(description = "모의고사 유형 목록", example = "[\"FIND_MATCH\", \"ESSAY\"]")
  private List<MockExamType> mockExamTypeList;

  @Override
  public boolean isValid() {
    return plainText != null && mockExamTypeList != null && !mockExamTypeList.isEmpty();
  }
}