package org.quizly.quizly.quiz.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.quizly.quizly.core.application.BaseRequest;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "문제 주제 변경 요청")
public class UpdateQuizzesTopicRequest implements BaseRequest {

  @Schema(description = "문제에 변결할 주제", example = "개발 방법론")
  private String topic;

  @Schema(description = "주제를 변경할 퀴즈 ID 목록", example = "[1, 3, 5]")
  private List<Long> quizIdList;

  @Override
  public boolean isValid() {
    return topic != null && !topic.trim().isEmpty() && quizIdList != null && !quizIdList.isEmpty();
  }
}
