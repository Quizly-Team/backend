package org.quizly.quizly.configuration.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.core.presentation.ErrorResponse;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "swagger.enabled")
public class SwaggerConfiguration {

  public static final String SECURITY_SCHEME_NAME = "BearerToken";

  @Bean
  public OpenAPI customOpenAPI(@Value("${springdoc.version}") String appVersion, @Value("${server.name}") String serverName) {
    return new OpenAPI()
        .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme()))
        .info(new Info().title(serverName).version(appVersion));
  }

  @Bean
  public OperationCustomizer operationCustomizer() {
    return (operation, handlerMethod) -> {
      ApiErrorCode apiErrorCode = handlerMethod.getMethodAnnotation(ApiErrorCode.class);
      if (apiErrorCode != null) {
        generateErrorResponseDocs(operation, apiErrorCode.errorCodes());
      }
      return operation;
    };
  }

  private void generateErrorResponseDocs(Operation operation, Class<? extends BaseErrorCode>[] errorCodes) {
    ApiResponses responses = operation.getResponses();

    Map<Integer, List<BaseErrorCode>> statusAndErrorCodes = groupErrorCodesByStatus(errorCodes);

    statusAndErrorCodes.forEach((status, codeList) -> {
      ApiResponse apiResponse = createErrorApiResponse(codeList);
      responses.addApiResponse(String.valueOf(status), apiResponse);
    });
  }

  private Map<Integer, List<BaseErrorCode>> groupErrorCodesByStatus(Class<? extends BaseErrorCode>[] errorCodes) {
    return Arrays.stream(errorCodes)
        .flatMap(enumClass -> Arrays.stream(enumClass.getEnumConstants()))
        .collect(Collectors.groupingBy(baseErrorCode -> baseErrorCode.getHttpStatus().value()));
  }

  private ApiResponse createErrorApiResponse(List<BaseErrorCode> codeList) {
    MediaType mediaType = new MediaType();
    codeList.forEach(baseErrorCode -> {
      ErrorResponse errorResponse = ErrorResponse.of(baseErrorCode);
      Example example = new Example().value(errorResponse);
      mediaType.addExamples(baseErrorCode.name(), example);
    });

    Content content = new Content().addMediaType("application/json", mediaType);
    return new ApiResponse().description("Error Response").content(content);
  }

  private SecurityScheme securityScheme() {
    return new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("KMS")
        .name(SECURITY_SCHEME_NAME);
  }
}
