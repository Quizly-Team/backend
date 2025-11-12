package org.quizly.quizly.external.storage.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StorageErrorCode implements BaseErrorCode<StorageException> {

    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "허용되지 않은 파일 형식입니다."),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "파일이 비어 있습니다."),
    MISSING_EXTENSION(HttpStatus.BAD_REQUEST, "파일 확장자가 없습니다."),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "파일 크기가 너무 큽니다."),
    FAILED_UPLOAD(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    FAILED_DELETE(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INVALID_MIME_TYPE(HttpStatus.BAD_REQUEST, "잘못된 MIME 타입의 파일입니다.");


    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public StorageException toException() {
        return new StorageException(httpStatus, message);
    }
}
