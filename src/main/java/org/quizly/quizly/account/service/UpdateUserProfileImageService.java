package org.quizly.quizly.account.service;

import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.account.service.ReadUserService.ReadUserRequest;
import org.quizly.quizly.account.service.ReadUserService.ReadUserResponse;
import org.quizly.quizly.core.domin.repository.UserRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.external.storage.service.ProfileImageService;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateUserProfileImageService implements BaseService<
        UpdateUserProfileImageService.UpdateUserProfileImageRequest,
        UpdateUserProfileImageService.UpdateUserProfileImageResponse> {

    private final ReadUserService readUserService;
    private final UserRepository userRepository;
    private final ProfileImageService profileImageService;

    @Override
    public UpdateUserProfileImageResponse execute(UpdateUserProfileImageRequest request) {
        if (request == null || !request.isValid()) {
            return UpdateUserProfileImageResponse.builder()
                    .success(false)
                    .errorCode(UpdateUserProfileImageErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
                    .build();
        }
        ReadUserResponse readUserResponse = readUserService.execute(
            ReadUserRequest.builder()
                .userPrincipal(request.getUserPrincipal())
                .build()
        );

        if (!readUserResponse.isSuccess()) {
            return UpdateUserProfileImageResponse.builder()
                    .success(false)
                    .errorCode(UpdateUserProfileImageErrorCode.NOT_FOUND_USER)
                    .build();
        }
        User user = readUserResponse.getUser();

        try {
            ProfileImageService.UploadProfileImageRequest uploadRequest =
                    ProfileImageService.UploadProfileImageRequest.builder()
                            .file(request.getFile())
                            .userId(user.getId())
                            .existingUrl(user.getProfileImageUrl())
                            .build();
            ProfileImageService.UploadProfileImageResponse uploadResponse =
                    profileImageService.execute(uploadRequest);

            if (!uploadResponse.isSuccess()) {
                return UpdateUserProfileImageResponse.builder()
                        .success(false)
                        .errorCode(UpdateUserProfileImageErrorCode.UPLOAD_FAILED)
                        .build();
            }

            String newUrl = uploadResponse.getFileUrl();
            user.setProfileImageUrl(newUrl);
            userRepository.save(user);

            return UpdateUserProfileImageResponse.builder()
                    .profileImageUrl(newUrl)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("[UpdateUserProfileImageService] 프로필 이미지 업로드 중 오류 발생", e);
            return UpdateUserProfileImageResponse.builder()
                    .success(false)
                    .errorCode(UpdateUserProfileImageErrorCode.UPLOAD_FAILED)
                    .build();
        }


    }

    @Getter
    @RequiredArgsConstructor
    public enum UpdateUserProfileImageErrorCode implements BaseErrorCode<DomainException> {
        NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
        NOT_FOUND_USER(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
        UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드에 실패했습니다.");

        private final HttpStatus httpStatus;
        private final String message;

        @Override
        public DomainException toException() {
            return new DomainException(httpStatus, this);
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class UpdateUserProfileImageRequest implements BaseRequest {
        private MultipartFile file;
        private UserPrincipal userPrincipal;

        @Override
        public boolean isValid() {
            return file != null && !file.isEmpty() && userPrincipal != null;
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @ToString
    public static class UpdateUserProfileImageResponse
            extends BaseResponse<UpdateUserProfileImageErrorCode> {
        private String profileImageUrl;
    }
}
