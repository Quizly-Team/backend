package org.quizly.quizly.account.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.account.service.ReadUserInfoService.ReadUserInfoRequest;
import org.quizly.quizly.account.service.ReadUserInfoService.ReadUserInfoResponse;
import org.quizly.quizly.account.service.ReadUserService.ReadUserRequest;
import org.quizly.quizly.account.service.ReadUserService.ReadUserResponse;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.domin.entity.User;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadUserInfoService implements BaseService<ReadUserInfoRequest, ReadUserInfoResponse> {

  private final ReadUserService readUserService;

  @Override
  public ReadUserInfoResponse execute(ReadUserInfoRequest request) {
    if (request == null || !request.isValid()) {
      return ReadUserInfoResponse.builder()
          .success(false)
          .errorCode(ReadUserInfoErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    ReadUserResponse readUserResponse = readUserService.execute(
        ReadUserRequest.builder()
            .userPrincipal(request.getUserPrincipal())
            .build()
    );

    if (!readUserResponse.isSuccess()) {
      return ReadUserInfoResponse.builder()
          .success(false)
          .errorCode(ReadUserInfoErrorCode.NOT_FOUND_USER)
          .build();
    }
    User user = readUserResponse.getUser();

    return ReadUserInfoResponse.builder()
        .name(user.getName())
        .nickName(user.getNickName())
        .email(user.getEmail())
        .profileImageUrl(user.getProfileImageUrl())
        .success(true)
        .build();
  }

  @Getter
  @RequiredArgsConstructor
  public enum ReadUserInfoErrorCode implements BaseErrorCode<DomainException> {

    NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
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
  public static class ReadUserInfoRequest implements BaseRequest {

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
  public static class ReadUserInfoResponse extends BaseResponse<ReadUserInfoErrorCode> {
    private String name;
    private String nickName;
    private String email;
    private String profileImageUrl;
  }

}
