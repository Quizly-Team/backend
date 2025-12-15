package org.quizly.quizly.external.ocr.invoker;

import lombok.RequiredArgsConstructor;
import org.quizly.quizly.external.ocr.service.ClovaOcrService;
import org.quizly.quizly.external.ocr.service.ExtractTextFromOcrService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class OcrAsyncInvoker {

    private final ExtractTextFromOcrService extractTextFromOcrService;

    @Async("mockExamTaskExecutor")
    public CompletableFuture<ClovaOcrService.ClovaOcrResponse> invoke(
            ClovaOcrService.ClovaOcrRequest request
    ) {
        try {
            return CompletableFuture.completedFuture(
                    extractTextFromOcrService.execute(request)
            );
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

}
