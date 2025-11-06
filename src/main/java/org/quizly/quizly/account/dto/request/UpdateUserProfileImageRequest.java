package org.quizly.quizly.account.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "유저 프로필 이미지 변경 요청")
public class UpdateUserProfileImageRequest implements BaseRequest {
    private MultipartFile file;
    private UserPrincipal userPrincipal;

    @Override
    public boolean isValid() {
        return file != null && !file.isEmpty() && userPrincipal != null;
    }
}
