package org.quizly.quizly.mock.dto.response;

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
@Schema(description = "모의고사 생성 응답")
public class CreateMemberMockExamResponse extends BaseResponse<GlobalErrorCode> {

  private List<MockExamDetail> mockExamDetailList;

  public record MockExamDetail(
      @Schema(description = "문제", example = "수취 체제의 개편에 대한 올바른 설명을 모두 고르시오.\\n<보기>\\nㄱ. 영정법은 인조 때 풍흉에 관계없이 전세를 토지 1결당 쌀 4~6두로 고정했다.\\nㄴ. 대동법은 광해군 때 경기도에서 처음 실시되었으며, 쌀 12두로 대체되었다.\\nㄷ. 균역법은 영조 때 실시되었으며, 군포를 1년에 2필에서 1필로 줄여주었다.\\nㄹ. 이앙법의 확산으로 벼와 보리의 이모작이 감소했다.")
      String text,
      @Schema(description = "문제 타입 FIND_CORRECT:옳은 것 찾기, FIND_INCORRECT:옳지 않은 것 찾기, FIND_MATCH:정답 모두 고르기, ESSAY:서술형, SHORT_ANSWER:단답형, TRUE_FALSE:OX ", example = "FIND_MATCH")
      String type,
      @Schema(description = "ESSAY, SHORT_ANSWER, TRUE_FALSE 빈 배열 반환", example = "[\n"
          + "        \"ㄱ, ㄴ, ㄷ, ㄹ\",\n"
          + "        \"ㄱ, ㄴ, ㄷ\",\n"
          + "        \"ㄱ, ㄴ\",\n"
          + "        \"ㄷ\"\n"
          + "      ]")
      List<String> options,
      @Schema(description = "정답", example = "ㄱ, ㄴ, ㄷ")
      String answer,
      @Schema(description = "해설", example = "ㄱ, ㄴ, ㄷ은 옳은 설명입니다. ㄹ: 이앙법의 확산으로 벼와 보리의 이모작이 확산되었습니다.")
      String explanation

  ) {}
}