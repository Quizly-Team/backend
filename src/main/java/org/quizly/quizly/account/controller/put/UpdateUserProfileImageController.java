package org.quizly.quizly.account.controller.put;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.account.service.UpdateUserProfileImageService;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@Tag(name = "Account", description = "계정")
public class UpdateUserProfileImageController {

    private final UpdateUserProfileImageService updateUserProfileImageService;

    @Operation(
            summary = "유저 프로필 이미지 변경 API",
            description = "회원 전용 API로 현재 로그인 유저의 프로필 이미지를 변경합니다.\n\n회원 API로 요청 시 토큰이 필요합니다.",
            operationId = "/account/profileImage"
    )
    @PutMapping("/account/profileImage")
    public ResponseEntity<UpdateUserProfileImageService.UpdateUserProfileImageResponse> updateProfileImage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("file") MultipartFile file
    ) {
        UpdateUserProfileImageService.UpdateUserProfileImageRequest request =
                UpdateUserProfileImageService.UpdateUserProfileImageRequest.builder()
                        .file(file)
                        .userPrincipal(userPrincipal)
                        .build();

        UpdateUserProfileImageService.UpdateUserProfileImageResponse response =
                updateUserProfileImageService.execute(request);

        if (response == null || !response.isSuccess()) {
            if (response != null && response.getErrorCode() != null) {
                throw response.getErrorCode().toException();
            }
            throw UpdateUserProfileImageService.UpdateUserProfileImageErrorCode.UPLOAD_FAILED.toException();
        }

        return ResponseEntity.ok(response);
    }
}

