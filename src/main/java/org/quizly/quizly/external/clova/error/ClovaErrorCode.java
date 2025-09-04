package org.quizly.quizly.external.clova.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ClovaErrorCode implements BaseErrorCode<ClovaException> {

  FAILED_CREATE_CLOVA_REQUEST(HttpStatus.INTERNAL_SERVER_ERROR, "CLOVA 서버 요청 생성에 실패하였습니다."),
  EMPTY_CLOVA_RESPONSE_BODY(HttpStatus.INTERNAL_SERVER_ERROR, "Clova API로부터 비어있는 응답 본문을 받았습니다."),
  FAILED_READ_CLOVA_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "CLOVA 서버 응답 읽기에 실패하였습니다."),
  PROMPT_FILE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "프롬프트 파일을 찾을 수 없습니다."),
  CLOVA_NETWORK_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Clova API와 통신 중 네트워크 오류가 발생했습니다."),
  FAILED_PARSE_CLOVA_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "Clova API 응답을 JSON으로 파싱하는 데 실패하였습니다.");
  

  private final HttpStatus httpStatus;

  private final String message;

  @Override
  public ClovaException toException() {
    return new ClovaException(message);
  }

}
