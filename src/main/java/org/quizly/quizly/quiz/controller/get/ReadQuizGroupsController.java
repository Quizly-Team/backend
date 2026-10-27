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
import org.quizly.quizly.quiz.dto.request.ReadQuizGroupsRequest;
import org.quizly.quizly.quiz.dto.response.ReadQuizGroupsResponse;
import org.quizly.quizly.quiz.dto.response.ReadQuizGroupsResponse.QuizGroup;
import org.quizly.quizly.quiz.service.ReadQuizGroupsService;
import org.quizly.quizly.quiz.service.ReadQuizGroupsService.ReadQuizGroupsErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Quiz", description = "퀴즈")
public class ReadQuizGroupsController {

    private final ReadQuizGroupsService readQuizGroupsService;

    @Operation(
        summary = "문제 그룹 목록 조회 API",
        description = "회원 전용 API로 지정된 그룹화 기준에 따라 문제 그룹 목록과 그룹별 문제 수를 조회합니다. 이 API는 JWT 토큰을 필요로 합니다.\n\n"
            + "- `groupType`: date (날짜별), topic (주제별)\n"
            + "- `page`: 페이지 번호 (기본값 1)\n"
            + "- `pageSize`: 그룹 단위 페이지 크기 (기본값 10, 최대 100)\n"
            + "- 그룹에 속한 문제는 `/quizzes?group=` 으로 조회합니다.",
        operationId = "/quizzes/groups"
    )
    @GetMapping("/quizzes/groups")
    @ApiErrorCode(errorCodes = {GlobalErrorCode.class, ReadQuizGroupsErrorCode.class})
    public ResponseEntity<ReadQuizGroupsResponse> readQuizGroups(
        @ModelAttribute ReadQuizGroupsRequest request,
        @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ReadQuizGroupsService.ReadQuizGroupsResponse serviceResponse = readQuizGroupsService.execute(
            ReadQuizGroupsService.ReadQuizGroupsRequest.builder()
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

        List<QuizGroup> quizGroupList =
            Stream.ofNullable(serviceResponse.getQuizGroupCountList())
                .flatMap(List::stream)
                .map(groupCount -> new QuizGroup(groupCount.group(), groupCount.quizCount()))
                .toList();

        return ResponseEntity.ok(ReadQuizGroupsResponse.builder()
            .quizGroupList(quizGroupList)
            .pagination(serviceResponse.getPagination())
            .build());
    }
}
