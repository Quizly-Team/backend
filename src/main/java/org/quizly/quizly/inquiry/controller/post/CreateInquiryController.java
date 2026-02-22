package org.quizly.quizly.inquiry.controller.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.quizly.quizly.inquiry.dto.request.CreateInquiryRequest;
import org.quizly.quizly.inquiry.dto.response.CreateInquiryResponse;
import org.quizly.quizly.inquiry.service.CreateInquiryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Tag(name = "Inquiry", description = "문의")
public class CreateInquiryController {

    private final CreateInquiryService createInquiryService;

    @Operation(
        summary = "사용자 문의 등록 API",
        description = "서비스 문의 사항에 대해 사용자가 문의를 등록합니다.",
        operationId = "/inquiries"
    )
    @PostMapping("/inquiries")
    @ApiErrorCode(errorCodes = {GlobalErrorCode.class, CreateInquiryService.CreateInquiryErrorCode.class})
    public ResponseEntity<CreateInquiryResponse> createInquiry(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                                    @RequestBody CreateInquiryRequest request){
        CreateInquiryService.CreateInquiryResponse serviceResponse = createInquiryService.execute(
            CreateInquiryService.CreateInquiryRequest.builder()
                .userPrincipal(userPrincipal)
                .title(request.getTitle())
                .content(request.getContent())
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

        return ResponseEntity.ok(toResponse(serviceResponse));
    }

    private CreateInquiryResponse toResponse(CreateInquiryService.CreateInquiryResponse serviceResponse){
        return CreateInquiryResponse.builder()
            .inquiryId(serviceResponse.getInquiryId())
            .status(serviceResponse.getStatus())
            .build();
    }
}
