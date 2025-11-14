package org.quizly.quizly.account.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.error.GlobalErrorCode;

@Getter
@NoArgsConstructor
@ToString
@SuperBuilder
@Schema(description = "유저 정보 조회 응답")
public class ReadUserInfoResponse extends BaseResponse<GlobalErrorCode> {

  @Schema(description = "이름", example = "퀴즐리")
  private String name;

  @Schema(description = "닉네임", example = "에스F레소")
  private String nickName;

  @Schema(description = "이메일", example = "quizlystudy@gmail.com")
  private String email;

  @Schema(description = "프로필 url", example = "https://kr.object.ncloudstorage.com/quizly-profile-images/defaults/default_profile.png")
  private String profileImageUrl;

}
