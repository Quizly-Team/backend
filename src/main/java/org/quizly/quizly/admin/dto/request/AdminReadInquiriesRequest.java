package org.quizly.quizly.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.quizly.quizly.core.domin.entity.Inquiry;
import org.quizly.quizly.core.presentation.BasePaginationRequest;

@Getter
@Setter
@Schema(description = "관리자 문의 조회 요청")
public class AdminReadInquiriesRequest extends BasePaginationRequest {

    @Schema(description = "문의 답변 상태", example = "WAITING")
    private Inquiry.Status status;
}
