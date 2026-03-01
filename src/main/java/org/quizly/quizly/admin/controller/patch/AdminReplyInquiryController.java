package org.quizly.quizly.admin.controller.patch;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.admin.service.AdminReplyInquiryService;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자")
public class AdminReplyInquiryController {

    private final AdminReplyInquiryService adminReplyInquiryService;

    @Operation(
        summary = "관리자 문의 답변 등록 API",
        description = "사용자가 등록한 문의에 대해 답변을 등록합니다.\n\n",
        operationId = "/admin/inquiries"
    )
    @PatchMapping("/admin/inquiries/{inquiryId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiErrorCode(errorCodes = {GlobalErrorCode.class, AdminReplyInquiryService.AdminReplyInquiryErrorCode.class})
    public ResponseEntity<AdminReplyInquiryService.AdminReplyInquiryResponse> adminReplyInquiry(
        @PathVariable Long inquiryId,
        @RequestBody AdminReplyInquiryService.AdminReplyInquiryRequest request ) {

        AdminReplyInquiryService.AdminReplyInquiryResponse serviceResponse = adminReplyInquiryService.execute(
            AdminReplyInquiryService.AdminReplyInquiryRequest.builder()
                .inquiryId(inquiryId)
                .reply(request.getReply())
                .build()
        );

        if (serviceResponse == null || !serviceResponse.isSuccess()) {
            Optional.ofNullable(serviceResponse)
                .map(BaseResponse::getErrorCode)
                .ifPresentOrElse(errorCode -> {
                    throw errorCode.toException();
                }, () -> {
                    throw GlobalErrorCode.INTERNAL_ERROR.toException();
                });
        }

        return ResponseEntity.ok(serviceResponse);
    }

}
