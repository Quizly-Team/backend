package org.quizly.quizly.external.ocr.dto.Request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OcrRequestDto {

    private List<Image> images;
    private String requestId;
    private String version;
    private long timestamp;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Image {
        private String format;
        private String name;
    }
}

