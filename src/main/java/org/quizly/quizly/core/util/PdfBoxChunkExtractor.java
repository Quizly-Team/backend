package org.quizly.quizly.core.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;

@Log4j2
public class PdfBoxChunkExtractor {

    public static List<String> extractPageChunks(MultipartFile file) {
        List<String> chunks = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             PDDocument document = PDDocument.load(is)) {

            PDFTextStripper stripper = new PDFTextStripper();
            int pageCount = document.getNumberOfPages();

            for (int i = 1; i <= pageCount; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);

                String pageText = stripper.getText(document);

                if (pageText != null && !pageText.isBlank()) {
                    chunks.add(pageText.trim());
                }
            }

        } catch (Exception e) {
            log.error("[PdfBoxChunkExtractor] PDF parsing failed", e);
            throw new RuntimeException("PDFBox parsing failed");
        }

        return chunks;
    }
}