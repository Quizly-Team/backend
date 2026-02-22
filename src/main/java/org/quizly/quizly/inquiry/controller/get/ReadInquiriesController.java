package org.quizly.quizly.inquiry.controller.get;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.domin.entity.Inquiry;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.quizly.quizly.inquiry.dto.response.ReadInquiriesResponse;
import org.quizly.quizly.inquiry.service.ReadInquiriesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Tag(name = "Inquiry", description = "문의")
public class ReadInquiriesController {

    private final ReadInquiriesService readInquiriesService;

    @Operation(
        summary = "사용자 문의 조회 API",
        description = "사용자가 등록한 문의 사항을 조회합니다.",
        operationId = "/inquiries"
    )
    @GetMapping("/inquiries")
    @ApiErrorCode(errorCodes = {GlobalErrorCode.class, ReadInquiriesService.ReadInquiriesErrorCode.class})
    public ResponseEntity<ReadInquiriesResponse> readInquiries(@AuthenticationPrincipal UserPrincipal userPrincipal){
        ReadInquiriesService.ReadInquiriesResponse serviceResponse = readInquiriesService.execute(
            ReadInquiriesService.ReadInquiriesRequest.builder()
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

        return ResponseEntity.ok(toResponse(serviceResponse.getInquiryList()));
    }
    private ReadInquiriesResponse toResponse(List<Inquiry> inquiryList){
        return ReadInquiriesResponse.builder()
            .success(true)
            .inquiryList(
                inquiryList.stream()
                    .map(i->new ReadInquiriesResponse.InquiryDetail(
                        i.getId(),
                        i.getTitle(),
                        i.getContent(),
                        i.getReply(),
                        i.getRepliedAt(),
                        i.getStatus(),
                        i.getCreatedAt(),
                        i.getUpdatedAt()
                    ))
                    .toList()
            )
            .build();
    }

}
