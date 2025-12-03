package org.quizly.quizly.mock.controller.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.external.clova.dto.Response.Hcx007MockExamResponse;
import org.quizly.quizly.external.ocr.service.ClovaOcrService;
import org.quizly.quizly.external.ocr.service.ExtractTextFromOcrService;
import org.quizly.quizly.mock.dto.request.CreateMemberMockExamRequest;
import org.quizly.quizly.mock.dto.response.CreateMemberMockExamResponse;
import org.quizly.quizly.mock.service.CreateMemberMockExamService;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Tag(name = "Mock", description = "모의고사")
public class CreateMemberOcrMockExamController {

    private final ExtractTextFromOcrService extractTextFromOcrService;
    private final CreateMemberMockExamService createMemberMockExamService;

    @Operation(
            summary = "OCR 기반 회원 모의고사 생성 API",
            description = "회원 전용 API로 모의고사 문제를 생성 합니다.\n\n회원 API로 요청 시 토큰이 필요합니다.\n\n OCR 기반 모의고사 제작합니다.",
            operationId = "/mock/member/ocr"
    )
    @PostMapping(value = "/mock/member/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiErrorCode(errorCodes = {GlobalErrorCode.class, CreateMemberMockExamService.CreateMemberMockExamErrorCode.class})
    public ResponseEntity<CreateMemberMockExamResponse> createOcrMemberMockExam(
            @RequestParam("file") MultipartFile file,
            @RequestParam("mockExamTypeList") List<CreateMemberMockExamRequest.MockExamType> mockExamTypeList,
            @RequestParam(value = "quizCount", required = false) Integer quizCount,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ){
        ClovaOcrService.ClovaOcrRequest ocrRequest = ClovaOcrService.ClovaOcrRequest.builder()
                .file(file)
                .build();

        ClovaOcrService.ClovaOcrResponse ocrResponse = extractTextFromOcrService.execute(ocrRequest);
        String plainText = ocrResponse.getPlainText();

        if (ocrResponse == null || !ocrResponse.isSuccess()) {
            if (ocrResponse != null && ocrResponse.getErrorCode() != null) {
                throw ocrResponse.getErrorCode().toException();
            }
            throw GlobalErrorCode.INTERNAL_ERROR.toException();
        }

        CreateMemberMockExamService.CreateMemberMockExamResponse serviceResponse = createMemberMockExamService.execute(
                CreateMemberMockExamService.CreateMemberMockExamRequest.builder()
                        .plainText(plainText)
                        .mockExamTypeList(mockExamTypeList)
                        .userPrincipal(userPrincipal)
                        .quizCount(quizCount)
                        .build());

        if (serviceResponse == null || !serviceResponse.isSuccess()) {
            if (serviceResponse != null && serviceResponse.getErrorCode() != null) {
                throw serviceResponse.getErrorCode().toException();
            }
            throw GlobalErrorCode.INTERNAL_ERROR.toException();
        }

        return ResponseEntity.ok(toResponse(serviceResponse));
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
