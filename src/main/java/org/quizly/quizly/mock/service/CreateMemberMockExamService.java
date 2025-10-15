package org.quizly.quizly.mock.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.ResponseFormat;
import org.quizly.quizly.external.clova.service.CreateMockExamClovaStudioService.CreateMockExamClovaStudioRequest;
import org.quizly.quizly.external.clova.service.CreateMockExamClovaStudioService.CreateMockExamClovaStudioResponse;
import org.quizly.quizly.external.clova.util.ResponseFormatUtil;
import org.quizly.quizly.external.clova.dto.Response.Hcx007MockExamResponse;
import org.quizly.quizly.external.clova.service.CreateMockExamClovaStudioService;
import org.quizly.quizly.mock.dto.request.CreateMemberMockExamRequest.MockExamType;
import org.quizly.quizly.mock.dto.request.CreateMemberMockExamRequest.MockExamType.TypeCategory;
import org.quizly.quizly.mock.service.CreateMemberMockExamService.CreateMemberMockExamRequest;
import org.quizly.quizly.mock.service.CreateMemberMockExamService.CreateMemberMockExamResponse;
import org.quizly.quizly.oauth.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class CreateMemberMockExamService implements
    BaseService<CreateMemberMockExamRequest, CreateMemberMockExamResponse> {

  private final CreateMockExamClovaStudioService createMockExamClovaStudioService;

  private static final int DEFAULT_MOCK_EXAM_COUNT = 20;

  @Override
  public CreateMemberMockExamResponse execute(CreateMemberMockExamRequest request) {
    if (request == null || !request.isValid()) {
      return CreateMemberMockExamResponse.builder()
          .success(false)
          .errorCode(CreateMemberMockExamErrorCode.NOT_EXIST_REQUIRED_PARAMETER)
          .build();
    }

    Map<MockExamType, Integer> mockExamPlanMap = createMockExamPlanMap(request.getMockExamTypeList());
    List<Hcx007MockExamResponse> hcx007MockExamResponseList = new java.util.ArrayList<>();

    for (Map.Entry<MockExamType, Integer> entry : mockExamPlanMap.entrySet()) {
      List<Hcx007MockExamResponse> hcx007MockExamResponseListForType = createMockExam(
          entry.getKey(),
          entry.getValue(),
          request.getPlainText()
      );

      if (hcx007MockExamResponseListForType.isEmpty()) {
        log.error("[CreateMemberMockExamService] Failed to create Mock Exam from Clova Studio. MockExamType: {}", entry.getKey());
        return CreateMemberMockExamResponse.builder()
            .success(false)
            .errorCode(CreateMemberMockExamErrorCode.CLOVA_MOCK_EXAM_GENERATION_FAILED)
            .build();
      }

      if (entry.getKey().equals(MockExamType.FIND_MATCH)) {
        postProcessingFindMatch(hcx007MockExamResponseListForType);
      }

      hcx007MockExamResponseList.addAll(hcx007MockExamResponseListForType);
    }

    return CreateMemberMockExamResponse.builder()
      .quizList(hcx007MockExamResponseList)
      .success(true)
      .build();
  }



  private Map<MockExamType, Integer> createMockExamPlanMap(List<MockExamType> mockExamTypeList) {
    int numTypes = mockExamTypeList.size();
    Map<MockExamType, Integer> mockExamPlanMap = new LinkedHashMap<>();

    int baseCount = DEFAULT_MOCK_EXAM_COUNT / numTypes;
    int remainCount = DEFAULT_MOCK_EXAM_COUNT % numTypes;

    for (MockExamType type : mockExamTypeList) {
      mockExamPlanMap.put(type, baseCount);
    }

    for (int i = 0; i < remainCount; i++) {
      MockExamType currentType = mockExamTypeList.get(i);
      mockExamPlanMap.put(currentType, mockExamPlanMap.get(currentType) + 1);
    }

    return mockExamPlanMap;
  }

  private List<Hcx007MockExamResponse> createMockExam(MockExamType type, int quizNumber, String plainText) {
    String promptPath = type.getPromptPath();
    if (promptPath == null) {
      log.error("[CreateMemberMockExamService] Could not find a prompt path. MockExamType: {}", type);
      return Collections.emptyList();
    }
    ResponseFormat responseFormat;
    if(type.getTypeCategory() == TypeCategory.DESCRIPTIVE) {
      responseFormat = ResponseFormatUtil.createDescriptiveMockExamResponseFormat(quizNumber, quizNumber);
    } else {
      responseFormat = ResponseFormatUtil.createSelectionMockExamResponseFormat(quizNumber, quizNumber);
    }

    CreateMockExamClovaStudioResponse createMockExamClovaStudioResponse = createMockExamClovaStudioService.execute(
        CreateMockExamClovaStudioRequest.builder()
            .plainText(plainText)
            .promptPath(promptPath)
            .responseFormat(responseFormat)
            .build());

    if (createMockExamClovaStudioResponse != null && createMockExamClovaStudioResponse.isSuccess()) {
      return createMockExamClovaStudioResponse.getHcx007MockExamResponse();
    }

    log.error("[CreateMemberMockExamService] Failed to create mock exam from Clova Studio.");
    return Collections.emptyList();
  }

  private int countItems(String option) {
    if (option == null) {
      return 0;
    }
    return option.replaceAll("[\\s,]", "").length();
  }

  private void postProcessingFindMatch(List<Hcx007MockExamResponse> responseList) {
    responseList.stream()
        .filter(quiz -> quiz.getOptions() != null && !quiz.getOptions().contains(quiz.getAnswer()))
        .forEach(quiz -> {
          if (quiz.getOptions().isEmpty()) {
            quiz.getOptions().add(quiz.getAnswer());
          } else {
            quiz.getOptions().set(0, quiz.getAnswer());
          }
        });

    responseList.forEach(quiz -> {
      if (quiz.getOptions() != null) {
        quiz.getOptions().sort((o1, o2) -> {
          int count1 = countItems(o1);
          int count2 = countItems(o2);
          if (count1 != count2) {
            return Integer.compare(count1, count2);
          }
          return o1.compareTo(o2);
        });
      }
    });
  }

  @Getter
  @RequiredArgsConstructor
  public enum CreateMemberMockExamErrorCode implements BaseErrorCode<DomainException> {

    NOT_EXIST_REQUIRED_PARAMETER(HttpStatus.BAD_REQUEST, "요청 파라미터가 존재하지 않습니다."),
    NOT_EXIST_PROVIDER_ID(HttpStatus.BAD_REQUEST, "Provider ID가 존재하지 않습니다."),
    CLOVA_MOCK_EXAM_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CLOVA 서버에서 모의고사 생성에 실패하였습니다.");

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
  public static class CreateMemberMockExamRequest implements BaseRequest {

    private String plainText;

    private List<MockExamType> mockExamTypeList;

    private UserPrincipal userPrincipal;

    @Override
    public boolean isValid() {
      return plainText != null && !plainText.isEmpty() && mockExamTypeList != null && userPrincipal != null;
    }
  }

  @Getter
  @Setter
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class CreateMemberMockExamResponse extends
      BaseResponse<CreateMemberMockExamErrorCode> {
    private List<Hcx007MockExamResponse> quizList;
  }
}
