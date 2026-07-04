package org.quizly.quizly.mock.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.quizly.quizly.mock.dto.request.CreateMemberMockExamRequest.MockExamType;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedMockExamResponse {

    private String quiz;
    private MockExamType type;
    private List<String> options;
    private String answer;
    private String explanation;
}
