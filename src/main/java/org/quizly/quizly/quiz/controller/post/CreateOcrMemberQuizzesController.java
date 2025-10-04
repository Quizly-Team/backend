package org.quizly.quizly.quiz.controller.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.domin.entity.Quiz;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.external.ocr.service.ClovaOcrService;
import org.quizly.quizly.external.ocr.service.ExtractTextFromOcrService;
import org.quizly.quizly.oauth.UserPrincipal;
import org.quizly.quizly.quiz.dto.response.CreateQuizzesResponse;
import org.quizly.quizly.quiz.service.CreateMemberQuizzesService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Tag(name = "Quiz", description = "퀴즈")
public class CreateOcrMemberQuizzesController {

    private final ExtractTextFromOcrService extractTextFromOcrService;
    private final CreateMemberQuizzesService createMemberQuizzesService;

    @Operation(
            summary = "OCR 기반 회원 퀴즈 생성 API",
            description = "이미지(OCR) 기반으로 회원 전용 퀴즈 10문제를 생성합니다.\n\nplainText는 OCR에서 자동 추출합니다."
    )
    @PostMapping(value = "/quizzes/member/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreateQuizzesResponse> createOcrMemberQuizzes(
            @RequestPart("file") MultipartFile file,
            @RequestParam("type") Quiz.QuizType type,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ClovaOcrService.ClovaOcrRequest ocrRequest = ClovaOcrService.ClovaOcrRequest.builder()
                .file(file)
                .build();

        ClovaOcrService.ClovaOcrResponse ocrResponse = extractTextFromOcrService.execute(ocrRequest);

        String plainText = ocrResponse.getPlainText();

        CreateMemberQuizzesService.CreateMemberQuizzesResponse serviceResponse =
                createMemberQuizzesService.execute(
                        CreateMemberQuizzesService.CreateMemberQuizzesRequest.builder()
                                .plainText(plainText)
                                .type(type)
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

        return ResponseEntity.ok(toResponse(serviceResponse));
    }

    private CreateQuizzesResponse toResponse(CreateMemberQuizzesService.CreateMemberQuizzesResponse serviceResponse) {
        List<Quiz> quizList = serviceResponse.getQuizList();
        List<CreateQuizzesResponse.QuizDetail> quizDetailList = quizList.stream()
                .map(quiz -> new CreateQuizzesResponse.QuizDetail(
                        quiz.getId(),
                        quiz.getQuizText(),
                        quiz.getQuizType().name(),
                        quiz.getOptions(),
                        quiz.getAnswer(),
                        quiz.getExplanation(),
                        quiz.getTopic()))
                .toList();
        return CreateQuizzesResponse.builder()
                .quizDetailList(quizDetailList)
                .build();
    }
}
