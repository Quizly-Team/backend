package org.quizly.quizly.external.ocr.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OcrErrorCode implements BaseErrorCode<OcrApiException> {

    NOT_EXIST_OCR_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "OCR 요청 필수 파라미터가 존재하지 않습니다."),
    FAILED_CREATE_OCR_REQUEST(HttpStatus.INTERNAL_SERVER_ERROR, "OCR 서버 요청 생성에 실패하였습니다."),
    EMPTY_OCR_RESPONSE_BODY(HttpStatus.INTERNAL_SERVER_ERROR, "OCR API로부터 비어있는 응답 본문을 받았습니다."),
    FAILED_READ_OCR_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "OCR 서버 응답 읽기에 실패하였습니다."),
    OCR_NETWORK_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "OCR API와 통신 중 네트워크 오류가 발생했습니다."),
    FAILED_PARSE_OCR_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "OCR API 응답을 JSON으로 파싱하는 데 실패하였습니다."),
    OCR_RESPONSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OCR 응답이 null이거나 처리에 실패했습니다."),
    EXTRACTED_TEXT_EMPTY(HttpStatus.BAD_REQUEST, "OCR로 추출된 텍스트가 비어 있습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public OcrApiException toException() {
        return new OcrApiException(httpStatus, message);
    }
}
