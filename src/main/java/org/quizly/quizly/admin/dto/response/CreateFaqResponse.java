package org.quizly.quizly.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.domin.entity.Faq.FaqCategory;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;

@Getter
@SuperBuilder
@NoArgsConstructor
@Schema(description = "FAQ 등록 응답")
public class CreateFaqResponse extends BaseResponse<GlobalErrorCode> {

  @Schema(description = "등록된 FAQ ID", example = "1")
  private Long faqId;

  @Schema(description = "FAQ 카테고리", example = "SERVICE_INTRO")
  private FaqCategory category;

  @Schema(description = "FAQ 질문", example = "Quizly는 어떤 서비스인가요?")
  private String question;

  @Schema(description = "FAQ 답변", example = "Quizly는 AI 기반 퀴즈 생성 서비스입니다.")
  private String answer;
}
