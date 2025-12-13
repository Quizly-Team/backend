package org.quizly.quizly.mock.controller.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.external.clova.dto.Response.Hcx007MockExamResponse;
import org.quizly.quizly.mock.dto.request.CreateMemberMockExamRequest;
import org.quizly.quizly.mock.dto.response.CreateMemberMockExamResponse;
import org.quizly.quizly.mock.dto.response.CreateMemberMockExamResponse.MockExamDetail;
import org.quizly.quizly.mock.service.CreateMemberMockExamService;
import org.quizly.quizly.oauth.UserPrincipal;
import org.quizly.quizly.quiz.service.CreateMemberQuizzesService.CreateMemberQuizzesErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Mock", description = "모의고사")
public class CreateMemberMockExamController {

  private final CreateMemberMockExamService createMemberMockExamService;

  @Operation(
      summary = "텍스트 기반 회원 모의고사 생성 API",
      description = "회원 전용 API로 모의고사 문제를 생성 합니다.\n\n회원 API로 요청 시 토큰이 필요합니다.\n\n텍스트 기반 모의고사 제작합니다.",
      operationId = "/mock/member"
  )
  @PostMapping("/mock/member")
  @ApiErrorCode(errorCodes = {GlobalErrorCode.class, CreateMemberQuizzesErrorCode.class})
  public ResponseEntity<CreateMemberMockExamResponse> createMemberMockExam(
      @RequestBody CreateMemberMockExamRequest request,
      @AuthenticationPrincipal UserPrincipal userPrincipal) {

    CreateMemberMockExamService.CreateMemberMockExamResponse serviceResponse = createMemberMockExamService.execute(
        CreateMemberMockExamService.CreateMemberMockExamRequest.builder()
          .plainText(request.getPlainText())
          .mockExamTypeList(request.getMockExamTypeList())
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

    return ResponseEntity.ok(toResponse(serviceResponse));
  }

  private CreateMemberMockExamResponse toResponse(CreateMemberMockExamService.CreateMemberMockExamResponse serviceResponse) {
    List<Hcx007MockExamResponse> hcx007MockExamResponseList = serviceResponse.getQuizList();
    List<MockExamDetail> mockExamDetailList =  hcx007MockExamResponseList.stream()
        .map(mock -> new MockExamDetail(
            mock.getQuiz(),
            mock.getType().toString(),
            mock.getOptions(),
            mock.getAnswer(),
            mock.getExplanation()
        )).toList();
    return CreateMemberMockExamResponse.builder()
        .mockExamDetailList(mockExamDetailList)
        .build();
  }
}
