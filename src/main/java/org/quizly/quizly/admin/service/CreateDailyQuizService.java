package org.quizly.quizly.admin.service;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.admin.service.CreateDailyQuizService.CreateDailyQuizRequest;
import org.quizly.quizly.admin.service.CreateDailyQuizService.CreateDailyQuizResponse;
import org.quizly.quizly.admin.support.DailyQuizQuestionCommand;
import org.quizly.quizly.admin.support.DailyQuizQuestionValidator;
import org.quizly.quizly.admin.support.DailyQuizQuestionValidator.Result;
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

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class CreateDailyQuizService implements
    BaseService<CreateDailyQuizRequest, CreateDailyQuizResponse> {

    private final DailyQuizRepository dailyQuizRepository;
    private final DailyQuizQuestionValidator dailyQuizQuestionValidator;

    @Override
    public CreateDailyQuizResponse execute(CreateDailyQuizRequest request) {
        if (request == null || !request.isValid()) {
            return fail(CreateDailyQuizErrorCode.NOT_EXIST_REQUIRED_PARAMETER);
        }

        Result validationResult = dailyQuizQuestionValidator.validate(request.getQuestions());
        if (validationResult.hasViolation()) {
            return fail(toErrorCode(validationResult.getViolation()));
        }

        DailyQuiz dailyQuiz = DailyQuiz.builder()
            .topic(request.getTopic())
            .warmUp(request.getWarmUp())
            .sourceContent(request.getSourceContent())
            .published(request.getPublished() != null && request.getPublished())
            .build();
        dailyQuiz.replaceQuestions(validationResult.getQuestions());

        DailyQuiz savedDailyQuiz = dailyQuizRepository.save(dailyQuiz);

        log.info("[CreateDailyQuizService] 5분 상식 퀴즈 생성 완료 dailyQuizId={}, topic={}, published={}",
            savedDailyQuiz.getId(), savedDailyQuiz.getTopic(), savedDailyQuiz.getPublished());

        return CreateDailyQuizResponse.builder()
            .dailyQuiz(savedDailyQuiz)
            .build();
    }

    private CreateDailyQuizResponse fail(CreateDailyQuizErrorCode errorCode) {
        return CreateDailyQuizResponse.builder()
            .success(false)
            .errorCode(errorCode)
            .build();
    }

    private CreateDailyQuizErrorCode toErrorCode(DailyQuizQuestionValidator.Violation violation) {
        return switch (violation) {
            case INVALID_QUESTION_COUNT -> CreateDailyQuizErrorCode.INVALID_QUESTION_COUNT;
            case INVALID_QUESTION_NUMBER -> CreateDailyQuizErrorCode.INVALID_QUESTION_NUMBER;
            case INVALID_TRUE_FALSE_ANSWER -> CreateDailyQuizErrorCode.INVALID_TRUE_FALSE_ANSWER;
            case NOT_EXIST_REQUIRED_PARAMETER ->
                CreateDailyQuizErrorCode.NOT_EXIST_REQUIRED_PARAMETER;
        };
    }

    @Getter
    @RequiredArgsConstructor
    public enum CreateDailyQuizErrorCode implements BaseErrorCode<DomainException> {

        NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "필수 파라미터가 누락되었습니다."),
        INVALID_QUESTION_COUNT(HttpStatus.BAD_REQUEST, "문항은 3개여야 합니다."),
        INVALID_QUESTION_NUMBER(HttpStatus.BAD_REQUEST, "문항 번호는 1부터 3까지 중복 없이 지정해야 합니다."),
        INVALID_TRUE_FALSE_ANSWER(HttpStatus.BAD_REQUEST, "OX 문항의 정답은 O 또는 X여야 합니다.");

        private final HttpStatus httpStatus;
        private final String message;

        @Override
        public DomainException toException() {
            return new DomainException(httpStatus, this);
        }
    }

    @Getter
    @Builder
    public static class CreateDailyQuizRequest implements BaseRequest {

        private String topic;
        private String warmUp;
        private String sourceContent;
        private Boolean published;
        private List<DailyQuizQuestionCommand> questions;

        @Override
        public boolean isValid() {
            return topic != null && !topic.isBlank()
                && warmUp != null && !warmUp.isBlank()
                && sourceContent != null && !sourceContent.isBlank();
        }
    }

    @Getter
    @SuperBuilder
    public static class CreateDailyQuizResponse extends BaseResponse<CreateDailyQuizErrorCode> {

        private DailyQuiz dailyQuiz;
    }
}
