package org.quizly.quizly.account.service;

import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.external.gemini.service.CreateTextGeminiService;
import org.quizly.quizly.external.gemini.service.CreateTextGeminiService.CreateTextGeminiRequest;
import org.quizly.quizly.external.gemini.service.CreateTextGeminiService.CreateTextGeminiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class ReadAiAnalysisService implements
        BaseService<ReadAiAnalysisService.ReadAiAnalysisRequest,
                ReadAiAnalysisService.ReadAiAnalysisResponse> {

    private final CreateTextGeminiService createTextGeminiService;

    @Override
    public ReadAiAnalysisResponse execute(ReadAiAnalysisRequest request) {
        if (request == null || !request.isValid()) {
            return ReadAiAnalysisResponse.builder()
                    .success(false)
                    .errorCode(ReadAiAnalysisErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
                    .build();
        }

        CreateTextGeminiRequest geminiRequest = CreateTextGeminiRequest.builder()
                .inputText(request.getAnalysisTargetText())
                .promptPath(request.getPromptPath())
                .build();

        CreateTextGeminiResponse geminiResponse =
                createTextGeminiService.execute(geminiRequest);


        if (!geminiResponse.isSuccess()) {
            return ReadAiAnalysisResponse.builder()
                    .success(false)
                    .errorCode(ReadAiAnalysisErrorCode.GEMINI_ANALYSIS_FAILED)
                    .build();
        }

        return ReadAiAnalysisResponse.builder()
                .analysisResult(geminiResponse.getResultText())
                .success(true)
                .build();
    }

    @Getter
    @RequiredArgsConstructor
    public enum ReadAiAnalysisErrorCode
            implements BaseErrorCode<DomainException> {

        NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "분석 요청 파라미터가 없습니다."),
        GEMINI_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 분석에 실패했습니다.");

        private final HttpStatus httpStatus;
        private final String message;

        @Override
        public DomainException toException() {
            return new DomainException(httpStatus,this);
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadAiAnalysisRequest implements BaseRequest {
        private String analysisTargetText;
        private String promptPath;

        @Override
        public boolean isValid() {
            return analysisTargetText != null && !analysisTargetText.isBlank()
                    && promptPath != null && !promptPath.isBlank();
        }
    }

    @Getter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadAiAnalysisResponse
            extends BaseResponse<ReadAiAnalysisErrorCode> {

        private String analysisResult;
    }
}
