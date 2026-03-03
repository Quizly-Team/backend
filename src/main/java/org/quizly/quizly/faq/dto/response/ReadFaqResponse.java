package org.quizly.quizly.faq.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.domin.entity.Faq.FaqCategory;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;

@Getter
@NoArgsConstructor
@ToString
@SuperBuilder
@Schema(description = "FAQ 목록 조회 응답")
public class ReadFaqResponse extends BaseResponse<GlobalErrorCode> {

  @Schema(description = "카테고리별 FAQ 그룹 목록")
  private List<FaqCategoryGroup> faqCategoryGroupList;

  @Schema(description = "카테고리별 FAQ 그룹")
  public record FaqCategoryGroup(
      @Schema(description = "FAQ 카테고리", example = "SERVICE_INTRO")
      FaqCategory category,
      @Schema(description = "카테고리 설명", example = "서비스 소개")
      String description,
      @Schema(description = "해당 카테고리의 FAQ 목록")
      List<FaqDetail> faqDetailList
  ) {}

  @Schema(description = "FAQ 상세 정보")
  public record FaqDetail(
      @Schema(description = "FAQ ID", example = "1")
      Long id,
      @Schema(description = "질문", example = "Quizly는 어떤 서비스인가요?")
      String question,
      @Schema(description = "답변", example = "Quizly는 AI 기반 퀴즈 생성 서비스입니다.")
      String answer
  ) {}
}
