package org.quizly.quizly.quiz.controller.get;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collections;
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
import org.quizly.quizly.oauth.UserPrincipal;
import org.quizly.quizly.quiz.dto.response.ReadQuizzesResponse;
import org.quizly.quizly.quiz.dto.response.ReadQuizzesResponse.QuizGroup;
import org.quizly.quizly.quiz.dto.response.ReadQuizzesResponse.QuizHistoryDetail;
import org.quizly.quizly.quiz.service.ReadQuizzesService;
import org.quizly.quizly.quiz.service.ReadQuizzesService.ReadQuizzesErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Quiz", description = "퀴즈")
public class ReadQuizzesController {

  private final ReadQuizzesService readQuizzesService;

  @Operation(
      summary = "문제 조회 API",
      description = "회원 전용 API로 지정된 그룹화 기준에 따라 퀴즈 목록을 조회합니다. 이 API는 JWT 토큰을 필요로 합니다.\n\n"
          + "- `groupType`: date (날짜별), topic (주제별)\n"
          + "- 기본값: 그룹 기준이 명시되지 않으면 date로 자동 그룹화됩니다.",
      operationId = "/quizzes"
  )
  @GetMapping("/quizzes")
  @ApiErrorCode(errorCodes = {GlobalErrorCode.class, ReadQuizzesErrorCode.class})
  public ResponseEntity<ReadQuizzesResponse> readQuizzes(
      @RequestParam(name = "groupType", defaultValue = "date") String groupType,
      @AuthenticationPrincipal UserPrincipal userPrincipal
  ) {
    ReadQuizzesService.ReadQuizzesResponse serviceResponse = readQuizzesService.execute(
        ReadQuizzesService.ReadQuizzesRequest.builder()
            .groupType(groupType)
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

    return ResponseEntity.ok(toResponse(serviceResponse.getQuizList(), solveHistoryMap, groupType));
  }


  private ReadQuizzesResponse toResponse(List<Quiz> quizList, Map<Quiz, SolveHistory> solveHistoryMap, String groupType) {
    Map<String, List<Quiz>> groupedQuizMap = quizList.stream()
        .collect(Collectors.groupingBy(getGroupingFunction(groupType)));

    List<QuizGroup> quizGroupList = groupedQuizMap.entrySet().stream()
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
                    history != null && Boolean.TRUE.equals(history.getIsCorrect())
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
        .build();
  }

  private Function<Quiz, String> getGroupingFunction(String groupType) {
    if ("topic".equalsIgnoreCase(groupType)) {
      return Quiz::getTopic;
    }
    return quiz -> quiz.getCreatedAt().toLocalDate().toString();
  }

}
