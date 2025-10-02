package org.quizly.quizly.external.ocr.service;

import lombok.RequiredArgsConstructor;
import org.quizly.quizly.external.ocr.dto.Response.OcrResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ExtractTextFromOcrService {

    private final ClovaOcrService clovaOcrService;

    public String extractPlainText(MultipartFile file) {
        OcrResponseDto response = clovaOcrService.extractTextFromImage(file);
        if (response == null || response.getFullText().isBlank()) {
            throw new IllegalArgumentException("OCR에서 텍스트를 추출할 수 없습니다.");
        }
        return response.getFullText();
    }
}
