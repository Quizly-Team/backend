package org.quizly.quizly.dailyquiz.service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domain.entity.DailyQuiz;
import org.quizly.quizly.core.domain.repository.DailyQuizRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.dailyquiz.service.ReadDailyQuizService.ReadDailyQuizRequest;
import org.quizly.quizly.dailyquiz.service.ReadDailyQuizService.ReadDailyQuizResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadDailyQuizService implements
    BaseService<ReadDailyQuizRequest, ReadDailyQuizResponse> {

    private final DailyQuizRepository dailyQuizRepository;

    @Override
    public ReadDailyQuizResponse execute(ReadDailyQuizRequest request) {
        List<Long> publishedIds = dailyQuizRepository.findAllPublishedIds();

        if (publishedIds.isEmpty()) {
            return fail();
        }

        Long selectedId = publishedIds.get(
            ThreadLocalRandom.current().nextInt(publishedIds.size()));

        DailyQuiz dailyQuiz = dailyQuizRepository.findByIdAndDeletedFalse(selectedId)
            .orElse(null);

        if (dailyQuiz == null) {
            return fail();
        }

        dailyQuiz.getQuestions().size();

        return ReadDailyQuizResponse.builder()
            .dailyQuiz(dailyQuiz)
            .build();
    }

    private ReadDailyQuizResponse fail() {
        return ReadDailyQuizResponse.builder()
            .success(false)
            .errorCode(ReadDailyQuizErrorCode.NOT_FOUND_PUBLISHED_DAILY_QUIZ)
            .build();
    }

    @Getter
    @RequiredArgsConstructor
    public enum ReadDailyQuizErrorCode implements BaseErrorCode<DomainException> {

        NOT_FOUND_PUBLISHED_DAILY_QUIZ(HttpStatus.NOT_FOUND, "발행 중인 5분 상식 퀴즈가 없습니다.");

        private final HttpStatus httpStatus;
        private final String message;

        @Override
        public DomainException toException() {
            return new DomainException(httpStatus, this);
        }
    }

    @Getter
    @Builder
    public static class ReadDailyQuizRequest implements BaseRequest {

    }

    @Getter
    @SuperBuilder
    public static class ReadDailyQuizResponse extends BaseResponse<ReadDailyQuizErrorCode> {

        private DailyQuiz dailyQuiz;
    }
}
