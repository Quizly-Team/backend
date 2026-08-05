package org.quizly.quizly.admin.controller.get;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.admin.dto.request.AdminReadDailyQuizzesRequest;
import org.quizly.quizly.admin.dto.response.AdminReadDailyQuizzesResponse;
import org.quizly.quizly.admin.dto.response.AdminReadDailyQuizzesResponse.AdminDailyQuizDetail;
import org.quizly.quizly.admin.service.AdminReadDailyQuizzesService;
import org.quizly.quizly.admin.service.AdminReadDailyQuizzesService.AdminReadDailyQuizzesErrorCode;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.domain.entity.DailyQuiz;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.core.presentation.Pagination;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자")
public class AdminReadDailyQuizzesController {

    private final AdminReadDailyQuizzesService adminReadDailyQuizzesService;

    @Operation(
        summary = "5분 상식 퀴즈 세트 목록 조회 API",
        description = "관리자 전용 API로 등록된 5분 상식 퀴즈 세트를 최신순으로 조회합니다.\n\n"
            + "- `page`: 페이지 번호 (기본값 1)\n"
            + "- `pageSize`: 페이지 크기 (기본값 10)\n"
            + "- 발행 대상을 고를 때 사용합니다. `published`가 true인 세트가 조회 API 노출 대상입니다.",
        operationId = "/admin/daily-quizzes"
    )
    @GetMapping("/admin/daily-quizzes")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiErrorCode(errorCodes = {GlobalErrorCode.class, AdminReadDailyQuizzesErrorCode.class})
    public ResponseEntity<AdminReadDailyQuizzesResponse> adminReadDailyQuizzes(
        @ModelAttribute AdminReadDailyQuizzesRequest request
    ) {
        AdminReadDailyQuizzesService.AdminReadDailyQuizzesResponse serviceResponse =
            adminReadDailyQuizzesService.execute(
                AdminReadDailyQuizzesService.AdminReadDailyQuizzesRequest.builder()
                    .pageRequest(request.toPageRequest())
                    .build()
            );

        if (serviceResponse == null || !serviceResponse.isSuccess()) {
            Optional.ofNullable(serviceResponse)
                .map(BaseResponse::getErrorCode)
                .ifPresentOrElse(errorCode -> {
                    throw errorCode.toException();
                }, () -> {
                    throw GlobalErrorCode.INTERNAL_ERROR.toException();
                });
        }

        return ResponseEntity.ok(
            toResponse(serviceResponse.getDailyQuizList(), serviceResponse.getPagination()));
    }

    private AdminReadDailyQuizzesResponse toResponse(
        List<DailyQuiz> dailyQuizList, Pagination pagination) {
        List<AdminDailyQuizDetail> details = dailyQuizList.stream()
            .map(dailyQuiz -> new AdminDailyQuizDetail(
                dailyQuiz.getId(),
                dailyQuiz.getTopic(),
                dailyQuiz.getPublished(),
                dailyQuiz.getWarmUp(),
                dailyQuiz.getCreatedAt(),
                dailyQuiz.getUpdatedAt()
            )).toList();

        return AdminReadDailyQuizzesResponse.builder()
            .dailyQuizList(details)
            .pagination(pagination)
            .build();
    }
}
