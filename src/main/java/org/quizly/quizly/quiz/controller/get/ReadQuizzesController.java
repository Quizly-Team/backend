package org.quizly.quizly.quiz.controller.get;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.quizly.quizly.quiz.dto.request.ReadQuizzesRequest;
import org.quizly.quizly.quiz.dto.response.ReadQuizzesResponse;
import org.quizly.quizly.quiz.dto.response.ReadQuizzesResponse.QuizHistoryDetail;
import org.quizly.quizly.quiz.service.ReadQuizzesService;
import org.quizly.quizly.quiz.service.ReadQuizzesService.ReadQuizzesErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Quiz", description = "퀴즈")
public class ReadQuizzesController {

    private final ReadQuizzesService readQuizzesService;

    @Operation(
        summary = "문제 조회 API",
        description = "회원 전용 API로 지정된 그룹에 속한 문제 목록을 문제 단위로 페이징하여 조회합니다. 이 API는 JWT 토큰을 필요로 합니다.\n\n"
            + "- `groupType`: date (날짜별), topic (주제별)\n"
            + "- `group`: 조회할 그룹 (date: yyyy-MM-dd, topic: 주제명), 필수\n"
            + "- `page`: 페이지 번호 (기본값 1)\n"
            + "- `pageSize`: 문제 단위 페이지 크기 (기본값 20, 최대 100)\n"
            + "- 그룹 목록은 `/quizzes/groups` 로 조회합니다.",
        operationId = "/quizzes"
    )
    @GetMapping("/quizzes")
    @ApiErrorCode(errorCodes = {GlobalErrorCode.class, ReadQuizzesErrorCode.class})
    public ResponseEntity<ReadQuizzesResponse> readQuizzes(
        @ModelAttribute ReadQuizzesRequest request,
        @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ReadQuizzesService.ReadQuizzesResponse serviceResponse = readQuizzesService.execute(
            ReadQuizzesService.ReadQuizzesRequest.builder()
                .groupType(request.getGroupType())
                .group(request.getGroup())
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

        List<QuizHistoryDetail> quizHistoryDetailList =
            Stream.ofNullable(serviceResponse.getSolveHistoryList())
                .flatMap(List::stream)
                .map(QuizHistoryDetail::from)
                .toList();

        return ResponseEntity.ok(ReadQuizzesResponse.builder()
            .group(request.getGroup())
            .quizHistoryDetailList(quizHistoryDetailList)
            .pagination(serviceResponse.getPagination())
            .build());
    }
}
