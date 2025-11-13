package org.quizly.quizly.account.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.quizly.quizly.core.application.BaseRequest;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "유저 닉네임 변경 요청")
public class UpdateUserNickNameRequest implements BaseRequest {

  @Schema(description = "변경할 닉네임", example = "퀴즐리")
  private String nickName;

  @Override
  public boolean isValid() {
    return nickName != null && !nickName.isBlank();
  }

}
