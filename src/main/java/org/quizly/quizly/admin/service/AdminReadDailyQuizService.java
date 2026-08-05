package org.quizly.quizly.admin.service;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.admin.service.AdminReadDailyQuizService.AdminReadDailyQuizRequest;
import org.quizly.quizly.admin.service.AdminReadDailyQuizService.AdminReadDailyQuizResponse;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domain.entity.DailyQuiz;
import org.quizly.quizly.core.domain.repository.DailyQuizRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReadDailyQuizService implements
    BaseService<AdminReadDailyQuizRequest, AdminReadDailyQuizResponse> {

    private final DailyQuizRepository dailyQuizRepository;

    @Override
    public AdminReadDailyQuizResponse execute(AdminReadDailyQuizRequest request) {
        if (request == null || !request.isValid()) {
            return AdminReadDailyQuizResponse.builder()
                .success(false)
                .errorCode(AdminReadDailyQuizErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
                .build();
        }

        DailyQuiz dailyQuiz = dailyQuizRepository
            .findByIdAndDeletedFalse(request.getDailyQuizId())
            .orElse(null);

        if (dailyQuiz == null) {
            return AdminReadDailyQuizResponse.builder()
                .success(false)
                .errorCode(AdminReadDailyQuizErrorCode.NOT_FOUND_DAILY_QUIZ)
                .build();
        }

        dailyQuiz.getQuestions().size();

        return AdminReadDailyQuizResponse.builder()
            .dailyQuiz(dailyQuiz)
            .build();
    }

    @Getter
    @RequiredArgsConstructor
    public enum AdminReadDailyQuizErrorCode implements BaseErrorCode<DomainException> {

        NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "필수 파라미터가 누락되었습니다."),
        NOT_FOUND_DAILY_QUIZ(HttpStatus.NOT_FOUND, "5분 상식 퀴즈를 찾을 수 없습니다.");

        private final HttpStatus httpStatus;
        private final String message;

        @Override
        public DomainException toException() {
            return new DomainException(httpStatus, this);
        }
    }

    @Getter
    @Builder
    public static class AdminReadDailyQuizRequest implements BaseRequest {

        private Long dailyQuizId;

        @Override
        public boolean isValid() {
            return dailyQuizId != null;
        }
    }

    @Getter
    @SuperBuilder
    public static class AdminReadDailyQuizResponse
        extends BaseResponse<AdminReadDailyQuizErrorCode> {

        private DailyQuiz dailyQuiz;
    }
}
