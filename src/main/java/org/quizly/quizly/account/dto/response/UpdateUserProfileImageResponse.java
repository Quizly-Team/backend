package org.quizly.quizly.account.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Schema(description = "유저 프로필 사진 링크 응답")
public class UpdateUserProfileImageResponse extends BaseResponse<GlobalErrorCode> {
    private String profileImageUrl;
}
