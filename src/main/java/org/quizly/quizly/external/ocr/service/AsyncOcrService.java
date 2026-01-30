package org.quizly.quizly.external.ocr.service;

import lombok.RequiredArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.quizly.quizly.core.application.BaseRequest;
import org.quizly.quizly.core.application.BaseResponse;
import org.quizly.quizly.core.application.BaseService;
import org.quizly.quizly.core.exception.DomainException;
import org.quizly.quizly.core.exception.error.BaseErrorCode;
import org.quizly.quizly.core.util.PdfBoxPageBatchExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Log4j2
@Component
@RequiredArgsConstructor
public class AsyncOcrService implements BaseService<AsyncOcrService.OcrExtractRequest, AsyncOcrService.OcrExtractResponse>{

    private final ExtractTextFromOcrService extractTextService;

    @Autowired
    @Qualifier("ocrTaskExecutor")
    private Executor ocrExecutor;

    @Override
    public OcrExtractResponse execute(OcrExtractRequest request) {

        if (request == null || !request.isValid()) {
            return OcrExtractResponse.builder()
                .success(false)
                .errorCode(OcrExtractErrorCode.NOT_EXIST_FILE)
                .build();
        }

        log.info("[AsyncOcrService] OCR extract start. Thread: {}",
            Thread.currentThread().getName());

        String mergedText = extractMergedPlainTextAsync(request.getFile()).join();

        return OcrExtractResponse.builder()
            .success(true)
            .plainText(mergedText)
            .build();
    }


    private CompletableFuture<String> extractMergedPlainTextAsync(MultipartFile file) {

        String contentType = file.getContentType();
        String extension = getFileExtension(file);

        boolean isPdf =
            contentType != null
                && "application/pdf".equalsIgnoreCase(contentType)
                && "pdf".equals(extension);

        boolean isImage =
            contentType != null
                && contentType.startsWith("image/")
                && List.of("jpg", "jpeg", "png", "bmp", "webp").contains(extension);

        if (isPdf) {
            try {
                List<MultipartFile> batches =
                    PdfBoxPageBatchExtractor.splitToPdfBatches(file);

                List<CompletableFuture<ClovaOcrService.ClovaOcrResponse>> futures =
                    batches.stream()
                        .map(this::callAsync)
                        .toList();

                return CompletableFuture
                    .allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        List<ClovaOcrService.ClovaOcrResponse> results = futures.stream()
                            .map(CompletableFuture::join)
                            .toList();

                        boolean hasFailure = results.stream()
                            .anyMatch(response -> !response.isSuccess());

                        if (hasFailure) {
                            throw OcrExtractErrorCode.OCR_PROCESS_FAILED.toException();
                        }

                        List<String> plainTexts = results.stream()
                            .map(ClovaOcrService.ClovaOcrResponse::getPlainText)
                            .toList();

                        return String.join("\n", plainTexts);
                    });

            } catch (Exception e) {
                log.error("[AsyncOcrService] PDF split failed", e);

                return CompletableFuture.failedFuture(
                    OcrExtractErrorCode.PDF_SPLIT_FAILED.toException()
                );
            }
        }
        if (isImage) {
            return callAsync(file)
                .thenApply(response -> {
                    if (!response.isSuccess()) {
                        throw OcrExtractErrorCode.OCR_PROCESS_FAILED.toException();
                    }
                    return response.getPlainText();
                });
        }

        return CompletableFuture.failedFuture(
            OcrExtractErrorCode.UNSUPPORTED_FILE_TYPE.toException()
        );
    }

    public CompletableFuture<ClovaOcrService.ClovaOcrResponse> callAsync(MultipartFile batch) {
        ClovaOcrService.ClovaOcrRequest request =
                ClovaOcrService.ClovaOcrRequest.builder()
                        .file(batch)
                        .build();

        return CompletableFuture.supplyAsync(
                () -> extractTextService.execute(request),
                ocrExecutor
        );
    }
    private String getFileExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
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
        NOT_EXIST_FILE(HttpStatus.BAD_REQUEST, "요청 파일이 존재하지 않습니다."),
        UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),
        PDF_SPLIT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PDF 페이지 분할에 실패했습니다."),
        OCR_PROCESS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OCR 처리에 실패했습니다.");

        private final HttpStatus httpStatus;
        private final String message;

        @Override
        public DomainException toException() {
            return new DomainException(httpStatus, this);
        }
    }
}
