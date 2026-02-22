package org.quizly.quizly.inquiry.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.domin.entity.Inquiry;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Schema(description = "문의 생성 응답")
public class CreateInquiryResponse extends BaseResponse<GlobalErrorCode> {

    @Schema(description = "문의 ID", example = "1")
    private Long inquiryId;

    @Schema(description = "문의 상태", example = "답변 대기중")
    private Inquiry.Status status;
}
