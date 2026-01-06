package org.quizly.quizly.mock.controller.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.external.clova.dto.Response.Hcx007MockExamResponse;
import org.quizly.quizly.external.ocr.service.AsyncOcrService;
import org.quizly.quizly.mock.service.CreateMemberMockExamService;
import org.quizly.quizly.mock.service.CreateMemberMockExamService.CreateMemberMockExamRequest;
import org.quizly.quizly.mock.dto.response.CreateMemberMockExamResponse;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
@RestController
@RequiredArgsConstructor
@Tag(name = "Mock", description = "모의고사")
public class CreateMemberOcrMockExamController {

    private final CreateMemberMockExamService createMemberMockExamService;
    private final AsyncOcrService asyncOcrService;
    @Operation(
            summary = "OCR 기반 회원 모의고사 생성 API",
            description = "회원 전용 API로 모의고사 문제를 생성 합니다.\n\n회원 API로 요청 시 토큰이 필요합니다.\n\n OCR 기반 모의고사 제작합니다.",
            operationId = "/mock/member/ocr"
    )
    @PostMapping(value = "/mock/member/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiErrorCode(errorCodes = {GlobalErrorCode.class, CreateMemberMockExamService.CreateMemberMockExamErrorCode.class})
    public ResponseEntity<CreateMemberMockExamResponse> createOcrMemberMockExam(
            @RequestParam("file") MultipartFile file,
            @RequestParam("mockExamTypeList") List<org.quizly.quizly.mock.dto.request.CreateMemberMockExamRequest.MockExamType> mockExamTypeList,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ){
        String plainText = asyncOcrService.execute(
            AsyncOcrService.OcrExtractRequest.builder()
                .file(file)
                .build()
        ).getPlainText();

        var response = createMemberMockExamService.execute(
                CreateMemberMockExamRequest.builder()
                        .plainText(plainText)
                        .mockExamTypeList(mockExamTypeList)
                        .userPrincipal(userPrincipal)
                        .build()
        );

        if (response == null || !response.isSuccess()) {
            if (response != null && response.getErrorCode() != null) {
                throw response.getErrorCode().toException();
            }
            throw GlobalErrorCode.INTERNAL_ERROR.toException();
        }

        return ResponseEntity.ok(toResponse(response));

    }



    private CreateMemberMockExamResponse toResponse(CreateMemberMockExamService.CreateMemberMockExamResponse serviceResponse) {
        List<Hcx007MockExamResponse> hcx007MockExamResponseList = serviceResponse.getQuizList();
        List<CreateMemberMockExamResponse.MockExamDetail> mockExamDetailList =  hcx007MockExamResponseList.stream()
                .map(mock -> new CreateMemberMockExamResponse.MockExamDetail(
                        mock.getQuiz(),
                        mock.getType().toString(),
                        mock.getOptions(),
                        mock.getAnswer(),
                        mock.getExplanation()
                )).toList();
        return CreateMemberMockExamResponse.builder()
                .mockExamDetailList(mockExamDetailList)
                .build();
    }


}
