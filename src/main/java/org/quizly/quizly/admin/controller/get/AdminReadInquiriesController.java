package org.quizly.quizly.admin.controller.get;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.admin.dto.request.AdminReadInquiriesRequest;
import org.quizly.quizly.admin.dto.response.AdminReadInquiriesResponse;
import org.quizly.quizly.admin.service.AdminReadInquiriesService;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.domain.entity.Inquiry;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.core.presentation.Pagination;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자")
public class AdminReadInquiriesController {

    private final AdminReadInquiriesService adminReadInquiriesService;

    @Operation(
        summary = "관리자 문의 전체 조회 API",
        description = "사용자가 등록한 문의를 전체 조회합니다.\n\n"
            + "- `page`: 페이지 번호 (기본값 1)\n"
            + "- `pageSize`: 그룹 단위 페이지 크기 (기본값 10)\n",
        operationId = "/admin/inquiries"
    )
    @GetMapping("/admin/inquiries")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiErrorCode(errorCodes = {GlobalErrorCode.class,
        AdminReadInquiriesService.AdminReadInquiriesErrorCode.class})
    public ResponseEntity<AdminReadInquiriesResponse> adminReadInquiries(
        @ModelAttribute AdminReadInquiriesRequest request) {
        AdminReadInquiriesService.AdminReadInquiriesResponse serviceResponse = adminReadInquiriesService.execute(
            AdminReadInquiriesService.AdminReadInquiriesRequest.builder()
                .status(request.getStatus())
                .pageRequest(request.toPageRequest())
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

        return ResponseEntity.ok(
            toResponse(serviceResponse.getInquiryList(), serviceResponse.getPagination()));
    }

    private AdminReadInquiriesResponse toResponse(
        List<Inquiry> inquiryList,
        Pagination pagination) {
        List<AdminReadInquiriesResponse.AdminInquiryDetail> details = inquiryList.stream()
            .map(inquiry -> new AdminReadInquiriesResponse.AdminInquiryDetail(
                inquiry.getId(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getUser().getName(),
                inquiry.getUser().getId(),
                inquiry.getReply(),
                inquiry.getRepliedAt(),
                inquiry.getStatus(),
                inquiry.getCreatedAt(),
                inquiry.getUpdatedAt()
            )).toList();

        return AdminReadInquiriesResponse.builder()
            .inquiryList(details)
            .pagination(pagination)
            .build();
    }

}
