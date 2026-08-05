package org.quizly.quizly.admin.service;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.admin.service.AdminReadDailyQuizzesService.AdminReadDailyQuizzesRequest;
import org.quizly.quizly.admin.service.AdminReadDailyQuizzesService.AdminReadDailyQuizzesResponse;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domain.entity.DailyQuiz;
import org.quizly.quizly.core.domain.repository.DailyQuizRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.core.presentation.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReadDailyQuizzesService implements
    BaseService<AdminReadDailyQuizzesRequest, AdminReadDailyQuizzesResponse> {

    private static final String SORT_BY_LATEST = "createdAt";

    private final DailyQuizRepository dailyQuizRepository;

    @Override
    public AdminReadDailyQuizzesResponse execute(AdminReadDailyQuizzesRequest request) {
        if (request == null || !request.isValid()) {
            return AdminReadDailyQuizzesResponse.builder()
                .success(false)
                .errorCode(AdminReadDailyQuizzesErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
                .build();
        }

        Pageable pageRequest = request.getPageRequest()
            .withSort(Sort.by(Sort.Direction.DESC, SORT_BY_LATEST));

        Page<DailyQuiz> dailyQuizPage = dailyQuizRepository.findAllByDeletedFalse(pageRequest);

        return AdminReadDailyQuizzesResponse.builder()
            .dailyQuizList(dailyQuizPage.getContent())
            .pagination(Pagination.getPaginationFromPage(dailyQuizPage))
            .build();
    }

    @Getter
    @RequiredArgsConstructor
    public enum AdminReadDailyQuizzesErrorCode implements BaseErrorCode<DomainException> {

        NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다.");

        private final HttpStatus httpStatus;
        private final String message;

        @Override
        public DomainException toException() {
            return new DomainException(httpStatus, this);
        }
    }

    @Getter
    @Builder
    public static class AdminReadDailyQuizzesRequest implements BaseRequest {

        private PageRequest pageRequest;

        @Override
        public boolean isValid() {
            return pageRequest != null;
        }
    }

    @Getter
    @SuperBuilder
    public static class AdminReadDailyQuizzesResponse
        extends BaseResponse<AdminReadDailyQuizzesErrorCode> {

        private List<DailyQuiz> dailyQuizList;
        private Pagination pagination;
    }
}
