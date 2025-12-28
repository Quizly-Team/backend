package org.quizly.quizly.external.ocr.service;

import lombok.RequiredArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.core.application.BaseAsyncService;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.core.util.PdfBoxPageBatchExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Log4j2
@Component
@RequiredArgsConstructor
public class AsyncOcrService implements BaseAsyncService<AsyncOcrService.OcrExtractRequest, AsyncOcrService.OcrExtractResponse> {

    private final ExtractTextFromOcrService extractTextService;

    @Autowired
    @Qualifier("ocrTaskExecutor")
    private Executor ocrExecutor;
    @Async("ocrTaskExecutor")
    @Override
    public CompletableFuture<OcrExtractResponse> execute(OcrExtractRequest request) {
        if (request == null || !request.isValid()) {
            return CompletableFuture.completedFuture(
                    OcrExtractResponse.builder()
                            .success(false)
                            .errorCode(OcrExtractErrorCode.NOT_EXIST_FILE)
                            .build()
            );
        }

        MultipartFile file = request.getFile();
        log.info("[AsyncOcrService] OCR extract start. Thread: {}", Thread.currentThread().getName());

        String mergedText = extractMergedPlainText(file);

        return CompletableFuture.completedFuture(
                OcrExtractResponse.builder()
                        .success(true)
                        .plainText(mergedText)
                        .build()
        );
    }

    public String extractMergedPlainText(MultipartFile file) {
        List<MultipartFile> batches = PdfBoxPageBatchExtractor.splitToPdfBatches(file);

        List<CompletableFuture<ClovaOcrService.ClovaOcrResponse>> futures =
                batches.stream()
                        .map(this::callAsync)
                        .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(ClovaOcrService.ClovaOcrResponse::isSuccess)
                .map(ClovaOcrService.ClovaOcrResponse::getPlainText)
                .collect(Collectors.joining("\n"));
    }

    @Async("ocrTaskExecutor")
    public CompletableFuture<ClovaOcrService.ClovaOcrResponse> callAsync(MultipartFile batch) {
        ClovaOcrService.ClovaOcrRequest request = ClovaOcrService.ClovaOcrRequest.builder()
                .file(batch)
                .build();

        return CompletableFuture.supplyAsync(
                () -> extractTextService.execute(request),
                ocrExecutor
        );
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class OcrExtractRequest implements BaseRequest {
        private MultipartFile file;

        @Override
        public boolean isValid() {
            return file != null && !file.isEmpty();
        }
    }

    @Getter
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class OcrExtractResponse extends BaseResponse<OcrExtractErrorCode>  {
        private String plainText;
    }

    @Getter
    @RequiredArgsConstructor
    public enum OcrExtractErrorCode implements BaseErrorCode<DomainException> {
        NOT_EXIST_FILE(HttpStatus.BAD_REQUEST, "요청 파일이 존재하지 않습니다.");

        private final HttpStatus httpStatus;
        private final String message;

        @Override
        public DomainException toException() {
            return new DomainException(httpStatus, this);
        }
    }
}
