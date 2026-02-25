package org.quizly.quizly.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.domin.entity.Inquiry;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@SuperBuilder
@NoArgsConstructor
@Schema(description = "관리자 문의 조회 응답")
public class AdminReadInquiriesResponse extends BaseResponse<GlobalErrorCode> {
    @Schema(description = "내 문의 목록")
    private List<AdminInquiryDetail> inquiryList;

    @Schema(description = "내 문의 목록 상세")
    public record AdminInquiryDetail(
        @Schema(description = "문의 ID", example = "1")
        Long inquiryId,
        @Schema(description = "문의 제목")
        String title,
        @Schema(description = "문의 내용")
        String content,
        @Schema(description = "작성자 이름")
        String writerName,
        @Schema(description = "작성자 ID")
        Long writerId,
        @Schema(description = "답변")
        String reply,
        @Schema(description = "답변 일자")
        LocalDateTime repliedAt,
        @Schema(description = "답변 상태")
        Inquiry.Status status,
        @Schema(description = "생성 일자")
        LocalDateTime createdAt,
        @Schema(description = "수정 일자")
        LocalDateTime updatedAt


    ){};
}
