package org.quizly.quizly.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;

@Getter
@SuperBuilder
@NoArgsConstructor
@Schema(description = "관리자 문의 답변 등록 응답")
public class AdminReplyInquiryResponse extends BaseResponse<GlobalErrorCode> {

    @Schema(description = "문의 ID", example = "1")
    private Long inquiryId;

    @Schema(description = "문의 제목", example = "로그인이 안 돼요.")
    private String title;

    @Schema(description = "등록된 답변 내용", example = "비밀번호 재설정을 시도해 보세요.")
    private String reply;

    @Schema(description = "답변 완료 시간", example = "2026-03-01 17:00:00")
    private String repliedAt;

}
