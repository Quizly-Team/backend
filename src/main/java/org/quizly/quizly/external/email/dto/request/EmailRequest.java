package org.quizly.quizly.external.email.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.quizly.quizly.core.application.BaseRequest;

import java.util.Map;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "이메일 발송 요청 DTO")
public class EmailRequest implements BaseRequest {

    @Schema(description = "수신자 이메일 주소", example = "user@example.com")
    private String to;

    @Schema(description = "메일 제목", example = "[Quizly] 문의하신 내용에 대한 답변입니다.")
    private String subject;

    @Schema(description = "사용할 HTML 템플릿 경로", example = "email/inquiry-reply")
    private String templatePath;

    @Schema(description = "템플릿에 주입할 변수 데이터")
    private Map<String, Object> variables;
}