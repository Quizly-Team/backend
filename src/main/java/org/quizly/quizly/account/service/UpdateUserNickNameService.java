package org.quizly.quizly.account.service;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.account.service.UpdateUserNickNameService.UpdateUserNickNameRequest;
import org.quizly.quizly.account.service.UpdateUserNickNameService.UpdateUserNickNameResponse;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.domin.repository.UserRepository;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateUserNickNameService implements BaseService<UpdateUserNickNameRequest, UpdateUserNickNameResponse> {

  private final UserRepository userRepository;

  @Override
  public UpdateUserNickNameResponse execute(UpdateUserNickNameRequest request) {
    if (request == null || !request.isValid()) {
      return UpdateUserNickNameResponse.builder()
          .success(false)
          .errorCode(UpdateUserNickNameErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    String providerId = request.getUserPrincipal().getProviderId();
    if (providerId == null || providerId.isEmpty()) {
      return UpdateUserNickNameResponse.builder()
          .success(false)
          .errorCode(UpdateUserNickNameErrorCode.NOT_EXIST_PROVIDER_ID)
          .build();
    }

    Optional<User> userOptional = userRepository.findByProviderId(providerId);
    if (userOptional.isEmpty()) {
      log.error("[UpdateUserNickNameService] User not found for providerId: {}", providerId);
      return UpdateUserNickNameResponse.builder()
          .success(false)
          .errorCode(UpdateUserNickNameErrorCode.NOT_FOUND_USER)
          .build();
    }
    User user = userOptional.get();

    String newNickName = request.getNickName();
    UpdateUserNickNameErrorCode validationError = validateNickName(newNickName);
    if (validationError != null) {
      log.warn("[UpdateUserNickNameService] Nickname validation failed: {}, error: {}", newNickName, validationError);
      return UpdateUserNickNameResponse.builder()
          .success(false)
          .errorCode(validationError)
          .build();
    }

    user.setNickName(newNickName);
    return UpdateUserNickNameResponse.builder()
        .success(true)
        .build();
  }

  private UpdateUserNickNameErrorCode validateNickName(String nickName) {
    if (nickName.length() < 2 || nickName.length() > 20) {
      return UpdateUserNickNameErrorCode.INVALID_NICKNAME_LENGTH;
    }

    return null;
  }

  @Getter
  @RequiredArgsConstructor
  public enum UpdateUserNickNameErrorCode implements BaseErrorCode<DomainException> {

    NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
    NOT_EXIST_PROVIDER_ID(HttpStatus.BAD_REQUEST, "Provider ID가 존재하지 않습니다."),
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    INVALID_NICKNAME_LENGTH(HttpStatus.BAD_REQUEST, "닉네임은 2자 이상 20자 이하여야 합니다.");

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
  public static class UpdateUserNickNameRequest implements BaseRequest {

    private String nickName;

    private UserPrincipal userPrincipal;

    @Override
    public boolean isValid() {
      return nickName != null && !nickName.isBlank() && userPrincipal != null;
    }
  }

  @Getter
  @Setter
  @SuperBuilder
  @NoArgsConstructor
  @ToString
  public static class UpdateUserNickNameResponse extends
      BaseResponse<UpdateUserNickNameErrorCode> {

  }

}
