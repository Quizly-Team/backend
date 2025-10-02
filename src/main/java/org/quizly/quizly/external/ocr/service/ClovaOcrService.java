package org.quizly.quizly.external.ocr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.quizly.quizly.external.ocr.dto.Request.OcrRequestDto;
import org.quizly.quizly.external.ocr.dto.Response.OcrResponseDto;
import org.quizly.quizly.external.ocr.error.OcrApiException;
import org.quizly.quizly.external.ocr.error.OcrErrorCode;
import org.quizly.quizly.external.ocr.property.OcrProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ClovaOcrService {

    private final OcrProperty ocrProperty;
    private final ObjectMapper objectMapper;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

    public OcrResponseDto extractTextFromImage(MultipartFile imageFile) {
        try {
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

            Request request = new Request.Builder()
                    .url(ocrProperty.getUrl())
                    .header("X-OCR-SECRET", ocrProperty.getSecret())
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw OcrErrorCode.OCR_NETWORK_ERROR.toException();
                }
                if (response.body() == null) {
                    throw OcrErrorCode.EMPTY_OCR_RESPONSE_BODY.toException();
                }

                try {
                    String responseBody = response.body().string();
                    return objectMapper.readValue(responseBody, OcrResponseDto.class);
                } catch (IOException e) {
                    throw OcrErrorCode.FAILED_PARSE_OCR_RESPONSE.toException();
                }
            }
        } catch (IOException e) {
            throw OcrErrorCode.FAILED_READ_OCR_RESPONSE.toException();
        }
    }


    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) return "";
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }
}
