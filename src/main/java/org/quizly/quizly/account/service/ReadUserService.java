package org.quizly.quizly.account.service;

import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
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

import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadUserService implements BaseService<ReadUserService.ReadUserRequest, ReadUserService.ReadUserResponse> {

  private final UserRepository userRepository;

  @Override
  public ReadUserResponse execute(ReadUserRequest request) {
    if (request == null || !request.isValid()) {
      return ReadUserResponse.builder()
          .success(false)
          .errorCode(ReadUserErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    String providerId = request.getUserPrincipal().getProviderId();
    if (providerId == null || providerId.isBlank()) {
      return ReadUserResponse.builder()
          .success(false)
          .errorCode(ReadUserErrorCode.NOT_EXIST_PROVIDER_ID)
          .build();
    }

    Optional<User> userOptional = userRepository.findByProviderId(providerId);
    if (userOptional.isEmpty()) {
      log.error("[ReadUserService] User not found for providerId: {}", providerId);
      return ReadUserResponse.builder()
          .success(false)
          .errorCode(ReadUserErrorCode.NOT_FOUND_USER)
          .build();
    }

    return ReadUserResponse.builder()
        .user(userOptional.get())
        .build();
  }

  @Getter
  @RequiredArgsConstructor
  public enum ReadUserErrorCode implements BaseErrorCode<DomainException> {
    NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
    NOT_EXIST_PROVIDER_ID(HttpStatus.BAD_REQUEST, "Provider ID가 존재하지 않습니다."),
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다.");

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
  public static class ReadUserRequest implements BaseRequest {
    private UserPrincipal userPrincipal;

    @Override
    public boolean isValid() {
      return userPrincipal != null;
    }
  }

  @Getter
  @Setter
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class ReadUserResponse extends BaseResponse<ReadUserErrorCode> {
    private User user;
  }
}
