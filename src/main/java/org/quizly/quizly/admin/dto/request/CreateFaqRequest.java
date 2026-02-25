package org.quizly.quizly.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.domin.entity.Faq.FaqCategory;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "FAQ 등록 요청")
public class CreateFaqRequest implements BaseRequest {

  @Schema(description = "FAQ 카테고리", example = "SERVICE_INTRO")
  private FaqCategory category;

  @Schema(description = "FAQ 질문", example = "Quizly는 어떤 서비스인가요?")
  private String question;

  @Schema(description = "FAQ 답변", example = "Quizly는 AI 기반 퀴즈 생성 서비스입니다.")
  private String answer;

  @Override
  public boolean isValid() {
    return category != null && question != null && answer != null;
  }
}
