package org.quizly.quizly.admin.service;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.admin.service.UpdateDailyQuizService.UpdateDailyQuizRequest;
import org.quizly.quizly.admin.service.UpdateDailyQuizService.UpdateDailyQuizResponse;
import org.quizly.quizly.admin.support.DailyQuizQuestionCommand;
import org.quizly.quizly.admin.support.DailyQuizQuestionValidator;
import org.quizly.quizly.admin.support.DailyQuizQuestionValidator.Result;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domain.entity.DailyQuiz;
import org.quizly.quizly.core.domain.entity.DailyQuizQuestion;
import org.quizly.quizly.core.domain.repository.DailyQuizRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateDailyQuizService implements
    BaseService<UpdateDailyQuizRequest, UpdateDailyQuizResponse> {

    private final DailyQuizRepository dailyQuizRepository;
    private final DailyQuizQuestionValidator dailyQuizQuestionValidator;

    @Override
    public UpdateDailyQuizResponse execute(UpdateDailyQuizRequest request) {
        if (request == null || !request.isValid()) {
            return fail(UpdateDailyQuizErrorCode.NOT_EXIST_REQUIRED_PARAMETER);
        }

        DailyQuiz dailyQuiz = dailyQuizRepository
            .findByIdAndDeletedFalse(request.getDailyQuizId())
            .orElse(null);

        if (dailyQuiz == null) {
            return fail(UpdateDailyQuizErrorCode.NOT_FOUND_DAILY_QUIZ);
        }

        List<DailyQuizQuestion> validatedQuestions = null;
        if (request.getQuestions() != null) {
            Result validationResult = dailyQuizQuestionValidator.validate(request.getQuestions());
            if (validationResult.hasViolation()) {
                return fail(toErrorCode(validationResult.getViolation()));
            }
            validatedQuestions = validationResult.getQuestions();
        }

        dailyQuiz.update(request.getTopic(), request.getWarmUp(), request.getSourceContent(),
            request.getPublished());

        if (validatedQuestions != null) {
            dailyQuiz.replaceQuestions(validatedQuestions);
            dailyQuizRepository.flush();
        }

        dailyQuiz.getQuestions().size();

        log.info("[UpdateDailyQuizService] 5분 상식 퀴즈 수정 완료 dailyQuizId={}, published={}",
            dailyQuiz.getId(), dailyQuiz.getPublished());

        return UpdateDailyQuizResponse.builder()
            .dailyQuiz(dailyQuiz)
            .build();
    }

    private UpdateDailyQuizResponse fail(UpdateDailyQuizErrorCode errorCode) {
        return UpdateDailyQuizResponse.builder()
            .success(false)
            .errorCode(errorCode)
            .build();
    }

    private UpdateDailyQuizErrorCode toErrorCode(DailyQuizQuestionValidator.Violation violation) {
        return switch (violation) {
            case INVALID_QUESTION_COUNT -> UpdateDailyQuizErrorCode.INVALID_QUESTION_COUNT;
            case INVALID_QUESTION_NUMBER -> UpdateDailyQuizErrorCode.INVALID_QUESTION_NUMBER;
            case INVALID_TRUE_FALSE_ANSWER -> UpdateDailyQuizErrorCode.INVALID_TRUE_FALSE_ANSWER;
            case NOT_EXIST_REQUIRED_PARAMETER ->
                UpdateDailyQuizErrorCode.NOT_EXIST_REQUIRED_PARAMETER;
        };
    }

    @Getter
    @RequiredArgsConstructor
    public enum UpdateDailyQuizErrorCode implements BaseErrorCode<DomainException> {

        NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "필수 파라미터가 누락되었습니다."),
        INVALID_QUESTION_COUNT(HttpStatus.BAD_REQUEST, "문항은 최소 1개 이상이어야 합니다."),
        INVALID_QUESTION_NUMBER(HttpStatus.BAD_REQUEST, "문항 번호는 1부터 문항 개수까지 중복 없이 지정해야 합니다."),
        INVALID_TRUE_FALSE_ANSWER(HttpStatus.BAD_REQUEST, "OX 문항의 정답은 O 또는 X여야 합니다."),
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
    public static class UpdateDailyQuizRequest implements BaseRequest {

        private Long dailyQuizId;
        private String topic;
        private String warmUp;
        private String sourceContent;
        private Boolean published;
        private List<DailyQuizQuestionCommand> questions;

        @Override
        public boolean isValid() {
            return dailyQuizId != null
                && hasAnyField()
                && isNotBlankIfPresent(topic)
                && isNotBlankIfPresent(warmUp)
                && isNotBlankIfPresent(sourceContent);
        }

        private boolean hasAnyField() {
            return topic != null || warmUp != null || sourceContent != null
                || published != null || questions != null;
        }

        private boolean isNotBlankIfPresent(String value) {
            return value == null || !value.isBlank();
        }
    }

    @Getter
    @SuperBuilder
    public static class UpdateDailyQuizResponse extends BaseResponse<UpdateDailyQuizErrorCode> {

        private DailyQuiz dailyQuiz;
    }
}
