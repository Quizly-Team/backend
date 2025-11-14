package org.quizly.quizly.account.controller.get;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.account.dto.response.ReadUserInfoResponse;
import org.quizly.quizly.account.service.ReadUserInfoService;
import org.quizly.quizly.account.service.ReadUserInfoService.ReadUserInfoErrorCode;
import org.quizly.quizly.account.service.ReadUserInfoService.ReadUserInfoRequest;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Account", description = "계정")
public class ReadUserInfoController {

  private final ReadUserInfoService readUserInfoService;

  @Operation(
      summary = "유저 정보 조회 API",
      description = "현재 로그인 유저의 정보를 조회합니다.",
      operationId = "/account"
  )
  @GetMapping("/account")
  @ApiErrorCode(errorCodes = {GlobalErrorCode.class, ReadUserInfoErrorCode.class})
  public ResponseEntity<ReadUserInfoResponse> readUserInfo(
      @AuthenticationPrincipal UserPrincipal userPrincipal
  ) {
    ReadUserInfoService.ReadUserInfoResponse serviceResponse = readUserInfoService.execute(
        ReadUserInfoRequest.builder()
            .userPrincipal(userPrincipal)
            .build()
    );

    if (serviceResponse == null) {
      throw GlobalErrorCode.INTERNAL_ERROR.toException();
    }
    if (!serviceResponse.isSuccess()) {
      throw serviceResponse.getErrorCode().toException();
    }

    return ResponseEntity.ok(toResponse(serviceResponse));
  }

  private ReadUserInfoResponse toResponse(ReadUserInfoService.ReadUserInfoResponse serviceResponse) {
    return ReadUserInfoResponse.builder()
        .name(serviceResponse.getName())
        .nickName(serviceResponse.getNickName())
        .email(serviceResponse.getEmail())
        .profileImageUrl(serviceResponse.getProfileImageUrl())
        .build();
  }
}
