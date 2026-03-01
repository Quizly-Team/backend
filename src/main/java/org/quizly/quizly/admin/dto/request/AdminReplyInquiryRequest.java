package org.quizly.quizly.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.application.BaseRequest;

@Getter
@SuperBuilder
@NoArgsConstructor
@Schema(description = "관리자 문의 답변 등록 요청")
public class AdminReplyInquiryRequest implements BaseRequest {

    @Schema(description = "문의 답변 내용", example = "문의하신 내용에 대한 답변입니다.")
    private String reply;

}
