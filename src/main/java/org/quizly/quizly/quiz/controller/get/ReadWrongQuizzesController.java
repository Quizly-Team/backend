package org.quizly.quizly.quiz.controller.get;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.domin.entity.Quiz;
import org.quizly.quizly.core.domin.entity.SolveHistory;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.core.presentation.Pagination;
import org.quizly.quizly.oauth.UserPrincipal;
import org.quizly.quizly.quiz.dto.request.ReadWrongQuizzesRequest;
import org.quizly.quizly.quiz.dto.response.ReadQuizzesResponse;
import org.quizly.quizly.quiz.dto.response.ReadQuizzesResponse.QuizGroup;
import org.quizly.quizly.quiz.dto.response.ReadQuizzesResponse.QuizHistoryDetail;
import org.quizly.quizly.quiz.service.ReadWrongQuizzesService;
import org.quizly.quizly.quiz.service.ReadWrongQuizzesService.ReadWrongQuizzesErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@Tag(name = "Quiz", description = "퀴즈")
public class ReadWrongQuizzesController {

  private final ReadWrongQuizzesService readWrongQuizzesService;

  @Operation(
      summary = "틀린 문제 조회 API",
      description = "회원 전용 API로 지정된 그룹화 기준에 따라 틀린 문제 목록을 조회합니다.\n\n이 API는 JWT 토큰을 필요로 합니다.\n\n"
          + "- `groupType`: date (날짜별), topic (주제별)\n"
          + "- `page`: 페이지 번호 (기본값 1)\n"
          + "- `pageSize`: 그룹 단위 페이지 크기 (기본값 10)\n"
          + "- 기본값: 그룹 기준이 명시되지 않으면 date로 자동 그룹화됩니다.",
      operationId = "/quizzes/wrong"
  )
  @GetMapping("/quizzes/wrong")
  @ApiErrorCode(errorCodes = {GlobalErrorCode.class, ReadWrongQuizzesErrorCode.class})
  public ResponseEntity<ReadQuizzesResponse> readWrongQuizzes(
      @ModelAttribute ReadWrongQuizzesRequest request,
      @AuthenticationPrincipal UserPrincipal userPrincipal
  ) {
    ReadWrongQuizzesService.ReadWrongQuizzesResponse serviceResponse = readWrongQuizzesService.execute(
        ReadWrongQuizzesService.ReadWrongQuizzesRequest.builder()
            .groupType(request.getGroupType())
            .pageRequest(request.toPageRequest())
            .userPrincipal(userPrincipal)
            .build());

    if (serviceResponse == null || !serviceResponse.isSuccess()) {
      Optional.ofNullable(serviceResponse)
          .map(BaseResponse::getErrorCode)
          .ifPresentOrElse(errorCode -> {
            throw errorCode.toException();
          }, () -> {
            throw GlobalErrorCode.INTERNAL_ERROR.toException();
          });
    }

    Map<Quiz, SolveHistory> solveHistoryMap =
        Stream.ofNullable(serviceResponse.getSolveHistoryList())
            .flatMap(List::stream)
            .collect(Collectors.toMap(SolveHistory::getQuiz, Function.identity(), (o1, o2) -> o1));

    return ResponseEntity.ok(
        toResponse(serviceResponse.getQuizList(), solveHistoryMap, request.getGroupType(), serviceResponse.getPagination()));
  }

  private ReadQuizzesResponse toResponse(
      List<Quiz> quizList,
      Map<Quiz, SolveHistory> solveHistoryMap,
      String groupType,
      Pagination pagination
  ) {
    Map<String, List<Quiz>> groupedQuizMap = quizList.stream()
        .collect(Collectors.groupingBy(getGroupingFunction(groupType)));

    Comparator<Map.Entry<String, List<Quiz>>> groupComparator =
        "topic".equalsIgnoreCase(groupType)
            ? Map.Entry.comparingByKey()
            : Map.Entry.<String, List<Quiz>>comparingByKey().reversed();

    List<QuizGroup> quizGroupList = groupedQuizMap.entrySet().stream()
        .sorted(groupComparator)
        .map(entry -> {
          List<QuizHistoryDetail> quizHistoryDetailList = entry.getValue().stream()
              .map(quiz -> {
                SolveHistory history = solveHistoryMap.get(quiz);
                return new QuizHistoryDetail(
                    quiz.getId(),
                    quiz.getQuizText(),
                    quiz.getQuizType().name(),
                    quiz.getOptions(),
                    quiz.getAnswer(),
                    quiz.getExplanation(),
                    quiz.getTopic(),
                    (history.getIsCorrect())
                );
              })
              .collect(Collectors.toList());

          return QuizGroup.builder()
              .group(entry.getKey())
              .quizHistoryDetailList(quizHistoryDetailList)
              .build();
        })
        .collect(Collectors.toList());

    return ReadQuizzesResponse.builder()
        .quizGroupList(quizGroupList)
        .pagination(pagination)
        .build();
  }

  private Function<Quiz, String> getGroupingFunction(String groupType) {
    if ("topic".equalsIgnoreCase(groupType)) {
      return Quiz::getTopic;
    }
    return quiz -> quiz.getCreatedAt().toLocalDate().toString();
  }

}
