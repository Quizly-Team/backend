package org.quizly.quizly.external.ocr.dto.Response;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class OcrResponseDto {
    private String version;
    private String requestId;
    private long timestamp;
    private List<ImageResult> images;

    @Getter
    @NoArgsConstructor
    public static class ImageResult {
        private List<Field> fields;
    }

    @Getter
    @NoArgsConstructor
    public static class Field {
        private String inferText;
    }

    public String getFullText() {
        if (images == null || images.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (ImageResult image : images) {
            if (image.getFields() != null) {
                for (Field field : image.getFields()) {
                    sb.append(field.getInferText()).append(" ");
                }
            }
        }
        return sb.toString().trim();
    }
}
