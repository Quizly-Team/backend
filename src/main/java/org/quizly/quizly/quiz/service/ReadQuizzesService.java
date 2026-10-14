package org.quizly.quizly.quiz.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Hibernate;
import org.quizly.quizly.account.service.ReadUserService;
import org.quizly.quizly.account.service.ReadUserService.ReadUserRequest;
import org.quizly.quizly.account.service.ReadUserService.ReadUserResponse;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domain.entity.SolveHistory;
import org.quizly.quizly.core.domain.entity.User;
import org.quizly.quizly.core.domain.repository.SolveHistoryRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.core.presentation.Pagination;
import org.quizly.quizly.oauth.UserPrincipal;
import org.quizly.quizly.quiz.service.ReadQuizzesService.ReadQuizzesRequest;
import org.quizly.quizly.quiz.service.ReadQuizzesService.ReadQuizzesResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class ReadQuizzesService implements BaseService<ReadQuizzesRequest, ReadQuizzesResponse> {

    private final ReadUserService readUserService;
    private final SolveHistoryRepository solveHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public ReadQuizzesResponse execute(ReadQuizzesRequest request) {
        if (request == null || !request.isValid()) {
            return ReadQuizzesResponse.builder()
                .success(false)
                .errorCode(ReadQuizzesErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
                .build();
        }

        ReadUserResponse readUserResponse = readUserService.execute(
            ReadUserRequest.builder()
                .userPrincipal(request.getUserPrincipal())
                .build()
        );

        if (!readUserResponse.isSuccess()) {
            return ReadQuizzesResponse.builder()
                .success(false)
                .errorCode(ReadQuizzesErrorCode.NOT_FOUND_USER)
                .build();
        }
        User user = readUserResponse.getUser();

        PageRequest pageRequest = request.getPageRequest();

        Page<SolveHistory> solveHistoryPage;
        if ("topic".equalsIgnoreCase(request.getGroupType())) {
            solveHistoryPage = solveHistoryRepository.findLatestSolveHistoriesByUserAndTopic(
                user, request.getGroup(), pageRequest);
        } else {
            LocalDate date;
            try {
                date = LocalDate.parse(request.getGroup());
            } catch (DateTimeParseException e) {
                log.warn("[ReadQuizzesService] 날짜 그룹 형식이 올바르지 않습니다. group={}",
                    request.getGroup());
                return ReadQuizzesResponse.builder()
                    .success(false)
                    .errorCode(ReadQuizzesErrorCode.INVALID_GROUP)
                    .build();
            }
            solveHistoryPage = solveHistoryRepository.findLatestSolveHistoriesByUserAndDate(
                user, date, pageRequest);
        }

        // 선택지는 @BatchSize 로 페이지 내 문제를 한 번에 조회한다
        solveHistoryPage.forEach(solveHistory ->
            Hibernate.initialize(solveHistory.getQuiz().getOptions()));

        return ReadQuizzesResponse.builder()
            .solveHistoryList(solveHistoryPage.getContent())
            .pagination(Pagination.getPaginationFromPage(solveHistoryPage))
            .build();
    }

    @Getter
    @RequiredArgsConstructor
    public enum ReadQuizzesErrorCode implements BaseErrorCode<DomainException> {

        NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
        INVALID_GROUP(HttpStatus.BAD_REQUEST, "그룹 형식이 올바르지 않습니다. 날짜 그룹은 yyyy-MM-dd 형식이어야 합니다."),
        NOT_FOUND_USER(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다.");

        private final HttpStatus httpStatus;
        private final String message;

        @Override
        public DomainException toException() {
            return new DomainException(httpStatus, this);
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class ReadQuizzesRequest implements BaseRequest {

        private String groupType;
        private String group;
        private UserPrincipal userPrincipal;
        private PageRequest pageRequest;

        @Override
        public boolean isValid() {
            return groupType != null && group != null && !group.isBlank()
                && userPrincipal != null && pageRequest != null;
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class ReadQuizzesResponse extends BaseResponse<ReadQuizzesErrorCode> {

        private List<SolveHistory> solveHistoryList;
        private Pagination pagination;
    }
}
