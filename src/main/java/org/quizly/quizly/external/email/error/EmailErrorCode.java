package org.quizly.quizly.external.email.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.springframework.http.HttpStatus;
@Getter
@RequiredArgsConstructor
public enum EmailErrorCode implements BaseErrorCode<EmailException> {
    NOT_EXIST_EMAIL_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "EMAIL 요청 필수 파라미터가 존재하지 않습니다."),
    FAILED_TO_SEND(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 발송에 실패했습니다.");


    private final HttpStatus httpStatus;

    private final String message;

    @Override
    public EmailException toException() {
        return new EmailException(message);
    }

}

