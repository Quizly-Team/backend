package org.quizly.quizly.inquiry.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.quizly.quizly.core.application.BaseRequest;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "문의 생성 요청")
public class CreateInquiryRequest implements BaseRequest {

    @Schema(description = "문의 제목" , example = "모의 고사 문제 제작 관련 기능을 추가 해주실 수 있나요?")
    private String title;

    @Schema(description = "문의 내용" , example = "모의 고사 문제를 만들고 나서, 새로 고침하면 이전에 만들었던 문제가 사라지는데,\n 이전에 만들었던 문제들을 쭉 모아볼 수 있는 기능을 추가해주실 수 있나요? ")
    private String content;

    @Override
    public boolean isValid(){
        return title != null && !title.isBlank() && content != null && !content.isBlank();
    }

}
