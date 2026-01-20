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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class ReadAiAnalysisService implements
        BaseService<ReadAiAnalysisService.ReadAiAnalysisRequest,
                ReadAiAnalysisService.ReadAiAnalysisResponse> {

    private final CreateAiAnalysisService createAiAnalysisService;
    private final AiAnalysisRepository aiAnalysisRepository;
    @Override
    public ReadAiAnalysisResponse execute(ReadAiAnalysisRequest request) {
        if (request == null || !request.isValid()) {
            return ReadAiAnalysisResponse.builder()
                    .success(false)
                    .errorCode(ReadAiAnalysisErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
                    .build();
        }



        User user = request.getUser();

        int solvedCount = request.getSolvedCount();

        if (solvedCount == 0) {
            return ReadAiAnalysisResponse.builder()
                .success(true)
                .analysisResult("아직 학습 기록이 없어요. 문제를 풀어보세요!")
                .build();
        }



        Optional<AiAnalysis> aiAnalysisOpt = aiAnalysisRepository.findByUser(user);

        if (aiAnalysisOpt.isEmpty() || aiAnalysisOpt.get().isDirty(solvedCount)) {
            CreateAiAnalysisService.CreateAiAnalysisResponse createResponse = createAiAnalysisService.execute(
                CreateAiAnalysisService.CreateAiAnalysisRequest.builder()
                    .user(user)
                    .solvedCount(solvedCount)
                    .analysisTargetText(request.getAnalysisTargetText())
                    .promptPath(request.getPromptPath())
                    .build()
            );

            if (!createResponse.isSuccess()) {
                return ReadAiAnalysisResponse.builder()
                    .success(false)
                    .errorCode(ReadAiAnalysisErrorCode.OPENAI_ANALYSIS_FAILED)
                    .build();
            }

            return ReadAiAnalysisResponse.builder()
                .success(true)
                .analysisResult(createResponse.getAnalysisResult())
                .build();
        }

        return ReadAiAnalysisResponse.builder()
            .success(true)
            .analysisResult(aiAnalysisOpt.get().getAnalysisResult())
            .build();
        }


    @Getter
    @RequiredArgsConstructor
    public enum ReadAiAnalysisErrorCode
            implements BaseErrorCode<DomainException> {

        NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "분석 요청 파라미터가 없습니다."),
        OPENAI_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 분석에 실패했습니다.");

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
        private User user;
        private int solvedCount;
        private String analysisTargetText;
        private String promptPath;

        @Override
        public boolean isValid() {
            return user != null
                && analysisTargetText != null && !analysisTargetText.isBlank()
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
