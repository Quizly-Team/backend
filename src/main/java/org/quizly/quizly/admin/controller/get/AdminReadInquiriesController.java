package org.quizly.quizly.admin.controller.get;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.quizly.quizly.admin.service.AdminReadInquiriesService;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.domin.entity.Inquiry;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자")
public class AdminReadInquiriesController {
    private final AdminReadInquiriesService adminReadInquiriesService;

    @Operation(
        summary = "관리자 문의 전체 조회 API",
        description = "사용자가 등록한 문의를 전체 조회합니다.\n\n",
        operationId = "/admin/inquiries"
    )
    @GetMapping("/admin/inquiries")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiErrorCode(errorCodes = {GlobalErrorCode.class, AdminReadInquiriesService.AdminReadInquiriesErrorCode.class})
    public ResponseEntity<AdminReadInquiriesService.AdminReadInquiriesResponse> adminReadInquiries(
        @AuthenticationPrincipal UserPrincipal userPrincipal,
        @RequestParam(required = false) Inquiry.Status status,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "DESC") Sort.Direction direction
        ){
        AdminReadInquiriesService.AdminReadInquiriesResponse serviceResponse = adminReadInquiriesService.execute(
            AdminReadInquiriesService.AdminReadInquiriesRequest.builder()
                .status(status)
                .sortBy(sortBy)
                .direction(direction)
                .userPrincipal(userPrincipal)
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
