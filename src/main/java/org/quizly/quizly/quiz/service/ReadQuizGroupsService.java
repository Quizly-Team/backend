package org.quizly.quizly.quiz.service;

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
import org.quizly.quizly.account.service.ReadUserService;
import org.quizly.quizly.account.service.ReadUserService.ReadUserRequest;
import org.quizly.quizly.account.service.ReadUserService.ReadUserResponse;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domain.entity.User;
import org.quizly.quizly.core.domain.repository.SolveHistoryRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.core.presentation.Pagination;
import org.quizly.quizly.oauth.UserPrincipal;
import org.quizly.quizly.quiz.service.ReadQuizGroupsService.ReadQuizGroupsRequest;
import org.quizly.quizly.quiz.service.ReadQuizGroupsService.ReadQuizGroupsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class ReadQuizGroupsService implements
    BaseService<ReadQuizGroupsRequest, ReadQuizGroupsResponse> {

    private final ReadUserService readUserService;
    private final SolveHistoryRepository solveHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public ReadQuizGroupsResponse execute(ReadQuizGroupsRequest request) {
        if (request == null || !request.isValid()) {
            return ReadQuizGroupsResponse.builder()
                .success(false)
                .errorCode(ReadQuizGroupsErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
                .build();
        }

        ReadUserResponse readUserResponse = readUserService.execute(
            ReadUserRequest.builder()
                .userPrincipal(request.getUserPrincipal())
                .build()
        );

        if (!readUserResponse.isSuccess()) {
            return ReadQuizGroupsResponse.builder()
                .success(false)
                .errorCode(ReadQuizGroupsErrorCode.NOT_FOUND_USER)
                .build();
        }
        User user = readUserResponse.getUser();

        PageRequest pageRequest = request.getPageRequest();

        Page<QuizGroupCount> groupPage;
        if ("topic".equalsIgnoreCase(request.getGroupType())) {
            groupPage = solveHistoryRepository.findQuizTopicGroupsByUser(user, pageRequest)
                .map(group -> new QuizGroupCount(group.getGroupKey(), group.getQuizCount()));
        } else {
            groupPage = solveHistoryRepository.findQuizDateGroupsByUser(user, pageRequest)
                .map(group -> new QuizGroupCount(group.getGroupKey().toString(),
                    group.getQuizCount()));
        }

        return ReadQuizGroupsResponse.builder()
            .quizGroupCountList(groupPage.getContent())
            .pagination(Pagination.getPaginationFromPage(groupPage))
            .build();
    }

    public record QuizGroupCount(String group, long quizCount) {

    }

    @Getter
    @RequiredArgsConstructor
    public enum ReadQuizGroupsErrorCode implements BaseErrorCode<DomainException> {

        NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
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
    public static class ReadQuizGroupsRequest implements BaseRequest {

        private String groupType;
        private UserPrincipal userPrincipal;
        private PageRequest pageRequest;

        @Override
        public boolean isValid() {
            return groupType != null && userPrincipal != null && pageRequest != null;
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class ReadQuizGroupsResponse extends BaseResponse<ReadQuizGroupsErrorCode> {

        private List<QuizGroupCount> quizGroupCountList;
        private Pagination pagination;
    }
}
