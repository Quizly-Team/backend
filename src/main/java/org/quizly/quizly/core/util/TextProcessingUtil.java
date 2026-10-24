package org.quizly.quizly.core.util;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.springframework.ai.document.Document;

@UtilityClass
public class TextProcessingUtil {

    private static final int MIN_CHUNK_SIZE = 400;
    private static final int MIN_TAIL_SIZE = 200;

    public static List<String> createChunkList(String text, int chunkSize, int chunkOverlap) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }

        CharacterTextSplitter splitter =
            new CharacterTextSplitter(chunkSize, chunkOverlap, MIN_CHUNK_SIZE, MIN_TAIL_SIZE);

        return splitter.split(new Document(text)).stream()
            .map(Document::getText)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
}
