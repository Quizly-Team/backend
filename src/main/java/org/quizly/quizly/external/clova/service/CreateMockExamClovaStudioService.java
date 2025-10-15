package org.quizly.quizly.external.clova.service;

import static org.quizly.quizly.core.util.okhttp.OkHttpRequest.createRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.util.TextResourceReaderUtil;
import org.quizly.quizly.core.util.okhttp.OkHttpJsonRequest;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.Message;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.Message.Content;
import org.quizly.quizly.external.clova.dto.Request.Hcx007Request.ResponseFormat;
import org.quizly.quizly.external.clova.dto.Response.Hcx007MockExamResponse;
import org.quizly.quizly.external.clova.error.ClovaErrorCode;
import org.quizly.quizly.external.clova.property.Hcx007Property;
import org.quizly.quizly.external.clova.service.CreateMockExamClovaStudioService.CreateMockExamClovaStudioRequest;
import org.quizly.quizly.external.clova.service.CreateMockExamClovaStudioService.CreateMockExamClovaStudioResponse;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class CreateMockExamClovaStudioService implements
    BaseService<CreateMockExamClovaStudioRequest, CreateMockExamClovaStudioResponse> {

  private final Hcx007Property hcx007Property;
  private final ObjectMapper objectMapper;
  private final TextResourceReaderUtil textResourceReaderUtil;

  @Override
  public CreateMockExamClovaStudioResponse execute(CreateMockExamClovaStudioRequest request) {
    if (request == null || !request.isValid()) {
      return CreateMockExamClovaStudioResponse.builder()
          .success(false)
          .errorCode(ClovaErrorCode.NOT_EXIST_CLOVA_REQUIRED_PARAMETER)
          .build();
    }
    String systemContent = getPrompt(request.getPromptPath());
    String requestBody = createClovaRequestBody(request.getPlainText(), systemContent, request.getResponseFormat());

    Request httpRequest = new Request.Builder()
        .url(hcx007Property.getUrl())
        .header("Authorization", "Bearer " + hcx007Property.getKey())
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
        .build();

    try (Response response = createRequest(httpRequest)) {
      if (response.body() == null) {
        log.error("[CreateMockExamClovaStudioService] response body is null. response code: {}", response.code());
        return CreateMockExamClovaStudioResponse.builder()
            .success(false)
            .errorCode(ClovaErrorCode.EMPTY_CLOVA_RESPONSE_BODY)
            .build();
      }

      String responseBody = response.body().string();

      if (!response.isSuccessful()) {
        log.error("[CreateMockExamClovaStudioService] Clova API returned non-successful code: {}, body: {}", response.code(), responseBody);
        return CreateMockExamClovaStudioResponse.builder()
            .success(false)
            .errorCode(ClovaErrorCode.FAILED_CREATE_CLOVA_REQUEST)
            .build();
      }

      try {
        com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(responseBody);
        String content = rootNode.path("result").path("message").path("content").asText();

        int startIndex = content.indexOf("[");
        int endIndex = content.lastIndexOf("]");
        if (startIndex == -1 || endIndex == -1 || startIndex > endIndex) {
          log.error("[CreateMockExamClovaStudioService] Failed to find JSON array in content. Original content: {}", content);
          return CreateMockExamClovaStudioResponse.builder()
              .success(false)
              .errorCode(ClovaErrorCode.FAILED_PARSE_CLOVA_RESPONSE)
              .build();
        }

        String jsonContent = content.substring(startIndex, endIndex + 1);

        List<Hcx007MockExamResponse> hcx007MockExamResponse = objectMapper.readValue(jsonContent, new com.fasterxml.jackson.core.type.TypeReference<List<Hcx007MockExamResponse>>() {});

        return CreateMockExamClovaStudioResponse.builder()
            .hcx007MockExamResponse(hcx007MockExamResponse)
            .build();

      } catch (IOException e) {
        log.error("[CreateMockExamClovaStudioService] Failed to parse Clova API response. Body: {}", responseBody, e);
        return CreateMockExamClovaStudioResponse.builder()
            .success(false)
            .errorCode(ClovaErrorCode.FAILED_READ_CLOVA_RESPONSE)
            .build();
      }

    } catch (IOException e) {
      log.error("[CreateMockExamClovaStudioService] Clova API request failed due to network I/O error.", e);
      return CreateMockExamClovaStudioResponse.builder()
          .success(false)
          .errorCode(ClovaErrorCode.CLOVA_NETWORK_ERROR)
          .build();
    }
  }

  private String createClovaRequestBody(String plainText, String systemContent, ResponseFormat responseFormat) {
    Message systemMessage = new Message("system", List.of(new Content("text", systemContent)));
    Message userMessage = new Message("user", List.of(new Content("text", "<입력으로 들어온 정리> " + plainText)));
    Hcx007Request hcx007Request = Hcx007Request.of(List.of(systemMessage, userMessage), responseFormat);

    String jsonBody = new OkHttpJsonRequest(hcx007Request).convertRequestToString();
    log.debug("Request JSON Body: {}", jsonBody);
    return jsonBody;
  }

  public String getPrompt(String promptPath) {
    String prompt = textResourceReaderUtil.load(promptPath);
    if (prompt == null || prompt.isBlank()) {
      throw ClovaErrorCode.PROMPT_FILE_NOT_FOUND.toException();
    }
    return prompt;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class CreateMockExamClovaStudioRequest implements BaseRequest {
    private String plainText;
    private String promptPath;
    private ResponseFormat responseFormat;

    @Override
    public boolean isValid() {
      return plainText != null && !plainText.isEmpty() && promptPath != null && !promptPath.isEmpty() && responseFormat != null;
    }
  }

  @Getter
  @Setter
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class CreateMockExamClovaStudioResponse extends BaseResponse<ClovaErrorCode> {
    private List<Hcx007MockExamResponse> hcx007MockExamResponse;
  }
}
