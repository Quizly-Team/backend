package org.quizly.quizly.external.clova.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.quizly.quizly.external.clova.dto.Response.Hcx007TopicResponse;
import org.quizly.quizly.external.clova.error.ClovaErrorCode;
import org.quizly.quizly.external.clova.property.Hcx007Property;
import org.quizly.quizly.external.clova.service.CreateTopicClovaStudioService.CreateTopicClovaStudioRequest;
import org.quizly.quizly.external.clova.service.CreateTopicClovaStudioService.CreateTopicClovaStudioResponse;
import org.quizly.quizly.external.clova.util.ResponseFormatUtil;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.quizly.quizly.core.util.okhttp.OkHttpRequest.createRequest;

@Log4j2
@Service
@RequiredArgsConstructor
public class CreateTopicClovaStudioService implements BaseService<CreateTopicClovaStudioRequest, CreateTopicClovaStudioResponse> {

  private final Hcx007Property hcx007Property;
  private final ObjectMapper objectMapper;
  private final TextResourceReaderUtil textResourceReaderUtil;

  @Override
  public CreateTopicClovaStudioResponse execute(CreateTopicClovaStudioRequest request) {
    if (request == null || !request.isValid()) {
      return CreateTopicClovaStudioResponse.builder()
          .success(false)
          .errorCode(ClovaErrorCode.NOT_EXIST_CLOVA_REQUIRED_PARAMETER)
          .build();
    }

    String systemContent = getPrompt(request.getPromptPath());
    String requestBody = createClovaRequestBody(request.getPlainText(), systemContent);

    Request httpRequest = new Request.Builder()
        .url(hcx007Property.getUrl())
        .header("Authorization", "Bearer " + hcx007Property.getKey())
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
        .build();

    try (Response response = createRequest(httpRequest)) {
      if (response.body() == null) {
        log.error("[CreateTopicClovaStudio] response body is null. response code: {}", response.code());
        return CreateTopicClovaStudioResponse.builder()
            .success(false)
            .errorCode(ClovaErrorCode.EMPTY_CLOVA_RESPONSE_BODY)
            .build();
      }

      String responseBody = response.body().string();

      if (!response.isSuccessful()) {
        log.error("[CreateTopicClovaStudio] Clova API returned non-successful code: {}, body: {}", response.code(), responseBody);
        return CreateTopicClovaStudioResponse.builder()
            .success(false)
            .errorCode(ClovaErrorCode.FAILED_CREATE_CLOVA_REQUEST)
            .build();
      }

      try {
        com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(responseBody);
        String content = rootNode.path("result").path("message").path("content").asText();

        Hcx007TopicResponse hcx007TopicResponse = parseTopicResponse(content);

        if (hcx007TopicResponse == null) {
          log.error("[CreateTopicClovaStudio] Failed to parse topic from content: {}", content);
          return CreateTopicClovaStudioResponse.builder()
              .success(false)
              .errorCode(ClovaErrorCode.FAILED_PARSE_CLOVA_RESPONSE)
              .build();
        }

        return CreateTopicClovaStudioResponse.builder()
            .topic(hcx007TopicResponse.getTopic())
            .success(true)
            .build();

      } catch (IOException e) {
        log.error("[CreateTopicClovaStudio] Failed to parse Clova API response. Body: {}", responseBody, e);
        return CreateTopicClovaStudioResponse.builder()
            .success(false)
            .errorCode(ClovaErrorCode.FAILED_READ_CLOVA_RESPONSE)
            .build();
      }

    } catch (IOException e) {
      log.error("[CreateTopicClovaStudio] Clova API request failed due to network I/O error.", e);
      return CreateTopicClovaStudioResponse.builder()
          .success(false)
          .errorCode(ClovaErrorCode.CLOVA_NETWORK_ERROR)
          .build();
    }
  }

  private String createClovaRequestBody(String plainText, String systemContent) {
    Message systemMessage = new Message("system", List.of(new Content("text", systemContent)));
    Message userMessage = new Message("user", List.of(new Content("text", "<입력으로 들어온 정리> " + plainText)));

    ResponseFormat responseFormat = ResponseFormatUtil.createTopicResponseFormat();
    Hcx007Request hcx007Request = Hcx007Request.of(List.of(systemMessage, userMessage), responseFormat);

    String jsonBody = new OkHttpJsonRequest(hcx007Request).convertRequestToString();
    log.debug("Request JSON Body: {}", jsonBody);
    return jsonBody;
  }

  private Hcx007TopicResponse parseTopicResponse(String content) {
    try {
      return objectMapper.readValue(content, Hcx007TopicResponse.class);
    } catch (IOException e) {
      log.debug("[CreateTopicClovaStudio] Direct parsing failed, trying regex extraction");
    }

    Pattern pattern = Pattern.compile("\"topic\"\\s*:\\s*\"([^\"]+)\"");;
    Matcher matcher = pattern.matcher(content);
    if (matcher.find()) {
      String topicValue = matcher.group(1);
      String validJson = String.format("{ \"topic\": \"%s\" }", topicValue);
      try {
        return objectMapper.readValue(validJson, Hcx007TopicResponse.class);
      } catch (IOException e) {
        log.error("[CreateTopicClovaStudio] Regex extraction failed", e);
      }
    }

    return null;
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
  public static class CreateTopicClovaStudioRequest implements BaseRequest {
    private String plainText;
    private String promptPath;

    @Override
    public boolean isValid() {
      return plainText != null && !plainText.isBlank() && promptPath != null && !promptPath.isBlank();
    }
  }

  @Getter
  @Setter
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  public static class CreateTopicClovaStudioResponse extends BaseResponse<ClovaErrorCode> {
    private String topic;
  }
}
