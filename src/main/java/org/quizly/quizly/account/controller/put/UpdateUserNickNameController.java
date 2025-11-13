package org.quizly.quizly.account.controller.put;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.account.dto.request.UpdateUserNickNameRequest;
import org.quizly.quizly.account.dto.response.UpdateUserNickNameResponse;
import org.quizly.quizly.account.service.UpdateUserNickNameService;
import org.quizly.quizly.account.service.UpdateUserNickNameService.UpdateUserNickNameErrorCode;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Account", description = "계정")
public class UpdateUserNickNameController {

  private final UpdateUserNickNameService updateUserNickNameService;

  @Operation(
      summary = "유저 닉네임 변경 API",
      description = "회원 전용 API로 현재 로그인 유저의 닉네임을 변경합니다.\n\n회원 API로 요청 시 토큰이 필요합니다.",
      operationId = "/account/nickname"
  )
  @PutMapping("/account/nickname")
  @ApiErrorCode(errorCodes = {GlobalErrorCode.class, UpdateUserNickNameErrorCode.class})
  public ResponseEntity<UpdateUserNickNameResponse> updateUserNickName (
      @RequestBody UpdateUserNickNameRequest request,
      @AuthenticationPrincipal UserPrincipal userPrincipal
  ) {

    UpdateUserNickNameService.UpdateUserNickNameResponse serviceResponse = updateUserNickNameService.execute(
        UpdateUserNickNameService.UpdateUserNickNameRequest.builder()
            .nickName(request.getNickName())
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

    return ResponseEntity.ok(UpdateUserNickNameResponse.builder().build());
  }

}
