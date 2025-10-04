package org.quizly.quizly.external.ocr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.experimental.SuperBuilder;
import okhttp3.*;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.external.ocr.dto.Request.OcrRequestDto;
import org.quizly.quizly.external.ocr.dto.Response.OcrResponseDto;
import org.quizly.quizly.external.ocr.error.OcrErrorCode;
import org.quizly.quizly.external.ocr.property.OcrProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import static org.quizly.quizly.core.util.okhttp.OkHttpRequest.createRequest;

@Service
@RequiredArgsConstructor
public class ClovaOcrService implements BaseService<ClovaOcrService.ClovaOcrRequest, ClovaOcrService.ClovaOcrResponse> {

    private final OcrProperty ocrProperty;
    private final ObjectMapper objectMapper;


    @Override
    public ClovaOcrResponse execute(ClovaOcrRequest request) {
        if (request == null || !request.isValid()) {
            return ClovaOcrResponse.builder()
                    .success(false)
                    .errorCode(OcrErrorCode.NOT_EXIST_OCR_REQUIRED_PARAMETER)
                    .build();
        }

        try {
            MultipartFile imageFile = request.getFile();
            String fileExtension = getFileExtension(imageFile.getOriginalFilename());

            OcrRequestDto ocrRequest = new OcrRequestDto(
                    Collections.singletonList(new OcrRequestDto.Image(fileExtension, "quiz-image")),
                    UUID.randomUUID().toString(),
                    "V2",
                    System.currentTimeMillis()
            );

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("message", objectMapper.writeValueAsString(ocrRequest))
                    .addFormDataPart("file", imageFile.getOriginalFilename(),
                            RequestBody.create(imageFile.getBytes(), MediaType.parse(imageFile.getContentType())))
                    .build();

            Request httpRequest = new Request.Builder()
                    .url(ocrProperty.getUrl())
                    .header("X-OCR-SECRET", ocrProperty.getSecret())
                    .post(requestBody)
                    .build();

            try (Response response = createRequest(httpRequest)) {
                if (!response.isSuccessful() || response.body() == null) {
                    return ClovaOcrResponse.builder()
                            .success(false)
                            .errorCode(OcrErrorCode.OCR_NETWORK_ERROR)
                            .build();
                }

                String responseBody = response.body().string();
                OcrResponseDto dto = objectMapper.readValue(responseBody, OcrResponseDto.class);

                return ClovaOcrResponse.builder()
                        .plainText(dto.getFullText())
                        .rawResponse(dto)
                        .success(true)
                        .build();
            }

        } catch (IOException e) {
            return ClovaOcrResponse.builder()
                    .success(false)
                    .errorCode(OcrErrorCode.FAILED_READ_OCR_RESPONSE)
                    .build();
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) return "";
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClovaOcrRequest implements BaseRequest {
        private MultipartFile file;

        @Override
        public boolean isValid() {
            return file != null && !file.isEmpty();
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClovaOcrResponse extends BaseResponse<OcrErrorCode> {
        private String plainText;
        private OcrResponseDto rawResponse;
    }
}
