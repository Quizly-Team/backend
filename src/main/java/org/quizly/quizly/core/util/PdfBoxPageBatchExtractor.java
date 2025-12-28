package org.quizly.quizly.core.util;

import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class PdfBoxPageBatchExtractor {

    private static final int BATCH_PAGE_SIZE = 10;

    public static List<MultipartFile> splitToPdfBatches(MultipartFile file) {
        List<MultipartFile> result = new ArrayList<>();

        try (PDDocument origin = PDDocument.load(file.getInputStream())) {
            int totalPages = origin.getNumberOfPages();


            for (int start = 0; start < totalPages; start += BATCH_PAGE_SIZE) {
                int end = Math.min(start + BATCH_PAGE_SIZE, totalPages);


                try (PDDocument batchDoc = new PDDocument();
                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                    for (int i = start; i < end; i++) {
                        batchDoc.addPage(origin.getPage(i));
                    }

                    batchDoc.save(out);

                    byte[] pdfBytes = out.toByteArray();

                    MultipartFile batchFile = new ByteArrayMultipartFile(
                            "file",
                            "batch_" + (start / BATCH_PAGE_SIZE) + ".pdf",
                            MediaType.APPLICATION_PDF_VALUE,
                            pdfBytes
                    );


                    result.add(batchFile);
                }
            }
        } catch (Exception e) {
            log.error("[PdfBoxPageBatchExtractor] PDF batch split failed", e);
            throw new RuntimeException("PDF batch split failed");
        }


        return result;
    }
}
