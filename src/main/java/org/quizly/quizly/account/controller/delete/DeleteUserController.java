package org.quizly.quizly.account.controller.delete;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quizly.quizly.account.service.DeleteUserService;
import org.quizly.quizly.account.service.DeleteUserService.DeleteUserErrorCode;
import org.quizly.quizly.account.service.DeleteUserService.DeleteUserRequest;
import org.quizly.quizly.configuration.swagger.ApiErrorCode;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Account", description = "계정")
public class DeleteUserController {

    private final DeleteUserService deleteUserService;

    @Operation(
        summary = "회원 탈퇴 API",
        description = "회원 전용 API로 현재 로그인 유저의 계정을 삭제 처리합니다.\n\n"
            + "삭제 시 개인정보는 즉시 파기(익명화)되며 복구할 수 없습니다. "
            + "동일한 소셜 계정으로 다시 로그인하면 신규 회원으로 가입됩니다.\n\n"
            + "회원 API로 요청 시 토큰이 필요합니다. "
            + "탈취된 accessToken만으로는 탈퇴가 불가능하도록, 로그인 시 발급된 refreshToken 쿠키가 "
            + "서버에 저장된 값과 일치하는지 함께 검증합니다.",
        operationId = "/account"
    )
    @DeleteMapping("/account")
    @ApiErrorCode(errorCodes = {GlobalErrorCode.class, DeleteUserErrorCode.class})
    public ResponseEntity<Void> deleteUser(
        @AuthenticationPrincipal UserPrincipal userPrincipal,
        @CookieValue("refreshToken") @Parameter(hidden = true) String refreshToken,
        HttpServletResponse response
    ) {

        var serviceResponse = deleteUserService.execute(
            DeleteUserRequest.builder()
                .userPrincipal(userPrincipal)
                .refreshToken(refreshToken)
                .build());

        if (!serviceResponse.isSuccess()) {
            Optional.of(serviceResponse)
                .map(BaseResponse::getErrorCode)
                .ifPresentOrElse(errorCode -> {
                    throw errorCode.toException();
                }, () -> {
                    throw GlobalErrorCode.INTERNAL_ERROR.toException();
                });
        }

        log.info("[ACCOUNT] Delete - userId: {}", userPrincipal.getUserId());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .sameSite("Strict")
            .maxAge(0)
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.noContent().build();
    }
}
