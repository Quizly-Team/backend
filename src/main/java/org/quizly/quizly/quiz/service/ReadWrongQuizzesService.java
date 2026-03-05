package org.quizly.quizly.quiz.service;

import java.time.LocalDate;
import java.util.Collections;
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
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domin.entity.Quiz;
import org.quizly.quizly.core.domin.entity.SolveHistory;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.account.service.ReadUserService;
import org.quizly.quizly.account.service.ReadUserService.ReadUserRequest;
import org.quizly.quizly.account.service.ReadUserService.ReadUserResponse;
import org.quizly.quizly.core.domin.repository.SolveHistoryRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.core.presentation.Pagination;
import org.quizly.quizly.oauth.UserPrincipal;
import org.quizly.quizly.quiz.service.ReadWrongQuizzesService.ReadWrongQuizzesRequest;
import org.quizly.quizly.quiz.service.ReadWrongQuizzesService.ReadWrongQuizzesResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class ReadWrongQuizzesService implements BaseService<ReadWrongQuizzesRequest, ReadWrongQuizzesResponse> {

  private final ReadUserService readUserService;
  private final SolveHistoryRepository solveHistoryRepository;

  @Override
  public ReadWrongQuizzesResponse execute(ReadWrongQuizzesRequest request) {
    if (request == null || !request.isValid()) {
      return ReadWrongQuizzesResponse.builder()
          .success(false)
          .errorCode(ReadWrongQuizzesErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    ReadUserResponse readUserResponse = readUserService.execute(
        ReadUserRequest.builder()
            .userPrincipal(request.getUserPrincipal())
            .build()
    );

    if (!readUserResponse.isSuccess()) {
      return ReadWrongQuizzesResponse.builder()
          .success(false)
          .errorCode(ReadWrongQuizzesErrorCode.NOT_FOUND_USER)
          .build();
    }
    User user = readUserResponse.getUser();

    PageRequest pageRequest = request.getPageRequest();

    List<SolveHistory> solveHistoryList;
    Pagination pagination;

    if ("topic".equalsIgnoreCase(request.getGroupType())) {
      Page<String> topicPage = solveHistoryRepository.findDistinctWrongQuizTopicsByUser(user, pageRequest);
      pagination = Pagination.getPaginationFromPage(topicPage);

      if (topicPage.hasContent()) {
        solveHistoryList = solveHistoryRepository.findLatestWrongSolveHistoriesByUserAndTopics(
            user, topicPage.getContent());
      } else {
        solveHistoryList = Collections.emptyList();
      }
    } else {
      Page<LocalDate> datePage = solveHistoryRepository.findDistinctWrongQuizDatesByUser(user, pageRequest);
      pagination = Pagination.getPaginationFromPage(datePage);

      if (datePage.hasContent()) {
        solveHistoryList = solveHistoryRepository.findLatestWrongSolveHistoriesByUserAndDates(
            user, datePage.getContent());
      } else {
        solveHistoryList = Collections.emptyList();
      }
    }

    List<Quiz> quizList = solveHistoryList.stream()
        .map(SolveHistory::getQuiz)
        .toList();

    return ReadWrongQuizzesResponse.builder()
        .quizList(quizList)
        .solveHistoryList(solveHistoryList)
        .pagination(pagination)
        .build();
  }

  @Getter
  @RequiredArgsConstructor
  public enum ReadWrongQuizzesErrorCode implements BaseErrorCode<DomainException> {

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
  public static class ReadWrongQuizzesRequest implements BaseRequest {

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
  public static class ReadWrongQuizzesResponse extends BaseResponse<ReadWrongQuizzesErrorCode> {

    private List<Quiz> quizList;
    private List<SolveHistory> solveHistoryList;
    private Pagination pagination;
  }
}
