package org.quizly.quizly.external.ocr.service;

import lombok.RequiredArgsConstructor;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.external.ocr.error.OcrErrorCode;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class ExtractTextFromOcrService implements BaseService<ClovaOcrService.ClovaOcrRequest, ClovaOcrService.ClovaOcrResponse> {

    private final ClovaOcrService clovaOcrService;

    @Override
    public ClovaOcrService.ClovaOcrResponse execute(ClovaOcrService.ClovaOcrRequest request) {
        ClovaOcrService.ClovaOcrResponse response = clovaOcrService.execute(request);
        if (!response.isSuccess() || response.getPlainText() == null || response.getPlainText().isBlank()) {
            return ClovaOcrService.ClovaOcrResponse.builder()
                    .success(false)
                    .errorCode(OcrErrorCode.EMPTY_OCR_RESPONSE_BODY)
                    .build();
        }
        return response;
    }
}

