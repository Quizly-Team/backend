package org.quizly.quizly.account.service;

import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domin.entity.AiAnalysis;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.repository.AiAnalysisRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.external.openai.service.CreateTextOpenAiService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class CreateAiAnalysisService implements BaseService<CreateAiAnalysisService.CreateAiAnalysisRequest,CreateAiAnalysisService.CreateAiAnalysisResponse> {

    private final CreateTextOpenAiService createTextOpenAiService;
    private final AiAnalysisRepository aiAnalysisRepository;
    @Override
    public CreateAiAnalysisService.CreateAiAnalysisResponse execute(CreateAiAnalysisRequest request) {
        if(request == null || !request.isValid()){
            return CreateAiAnalysisResponse.builder()
                .success(false)
                .errorCode(CreateAiAnalysisErrorCode.INVALID_PARAMETER)
                .build();
        }

        CreateTextOpenAiService.CreateTextOpenAiResponse openAiResponse = createTextOpenAiService.execute(
            CreateTextOpenAiService.CreateTextOpenAiRequest.builder()
                .inputText(request.analysisTargetText)
                .promptPath(request.getPromptPath())
                .build()
        );

        if(!openAiResponse.isSuccess()) {
            return CreateAiAnalysisResponse.builder()
                .success(false)
                .errorCode(CreateAiAnalysisErrorCode.ANALYSIS_FAILED)
                .build();
        }

        AiAnalysis aiAnalysis = aiAnalysisRepository
            .findByUser(request.getUser())
            .orElseGet(() -> AiAnalysis.builder()
                .user(request.getUser())
                .analysisDate(LocalDate.now())
                .build()
            );
        aiAnalysis.updateAfterAnalysis(request.getSolvedCount(), openAiResponse.getResultText());
        aiAnalysisRepository.save(aiAnalysis);

        return CreateAiAnalysisResponse.builder()
            .success(true)
            .analysisResult(openAiResponse.getResultText())
            .build();
    }

    @Getter
    @RequiredArgsConstructor
    public enum CreateAiAnalysisErrorCode implements BaseErrorCode<DomainException> {
        INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "부적절한 요청 파라미터입니다."),
        ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 분석 중 오류가 발생했습니다.");

        private final HttpStatus httpStatus;
        private final String message;

        @Override
        public DomainException toException() { return new DomainException(httpStatus, this); }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CreateAiAnalysisRequest implements BaseRequest {
        private final User user;
        private final int solvedCount;
        private final String analysisTargetText;
        private final String promptPath;

        @Override
        public boolean isValid() {
            return user != null && analysisTargetText != null && !analysisTargetText.isBlank() && promptPath != null && !promptPath.isBlank();
        }
    }

    @Getter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateAiAnalysisResponse extends BaseResponse<CreateAiAnalysisErrorCode> {
        private String analysisResult;
    }
}
