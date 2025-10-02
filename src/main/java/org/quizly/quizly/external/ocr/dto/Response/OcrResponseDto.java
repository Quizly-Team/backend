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
        if (images == null) {
            return "";
        }
        return images.stream()
                .filter(image -> image != null && image.getFields() != null)
                .flatMap(image -> image.getFields().stream())
                .filter(field -> field != null && field.getInferText() != null)
                .map(Field::getInferText)
                .collect(java.util.stream.Collectors.joining(" "));
    }
}
