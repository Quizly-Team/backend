package org.quizly.quizly.external.gemini.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GeminiErrorCode implements BaseErrorCode<GeminiException> {

    NOT_EXIST_GEMINI_REQUIRED_PARAMETER(
            HttpStatus.BAD_REQUEST,
            "Gemini 요청 필수 파라미터가 존재하지 않습니다."
    ),

    PROMPT_FILE_NOT_FOUND(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Gemini 프롬프트 파일을 찾을 수 없습니다."
    ),

    FAILED_GEMINI_REQUEST(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Gemini 서버 요청 생성에 실패하였습니다."
    ),

    EMPTY_GEMINI_RESPONSE_BODY(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Gemini API로부터 비어있는 응답 본문을 받았습니다."
    ),

    FAILED_PARSE_GEMINI_RESPONSE(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Gemini API 응답을 JSON으로 파싱하는 데 실패하였습니다."
    ),

    GEMINI_NETWORK_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Gemini API와 통신 중 네트워크 오류가 발생했습니다."
    );

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public GeminiException toException() {
        return new GeminiException(message);
    }
}
