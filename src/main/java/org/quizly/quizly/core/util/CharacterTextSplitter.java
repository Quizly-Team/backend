package org.quizly.quizly.core.util;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.transformer.splitter.TextSplitter;

public class CharacterTextSplitter extends TextSplitter {

    private static final List<String> CUT_SEPARATORS = List.of("\n\n", "\n", ". ", "? ", "! ", " ");
    private static final List<String> SENTENCE_SEPARATORS = List.of("\n", ". ", "? ", "! ");

    private final int chunkSize;
    private final int chunkOverlap;
    private final int minChunkSize;
    private final int minTailSize;

    public CharacterTextSplitter(int chunkSize, int chunkOverlap, int minChunkSize, int minTailSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize는 0보다 커야 합니다.");
        }
        if (chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("chunkOverlap은 0 이상, chunkSize 미만이어야 합니다.");
        }
        if (minChunkSize < 0 || minChunkSize >= chunkSize) {
            throw new IllegalArgumentException("minChunkSize는 0 이상, chunkSize 미만이어야 합니다.");
        }
        if (minTailSize < 0 || minTailSize >= chunkSize) {
            throw new IllegalArgumentException("minTailSize는 0 이상, chunkSize 미만이어야 합니다.");
        }
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.minChunkSize = minChunkSize;
        this.minTailSize = minTailSize;
    }

    @Override
    protected List<String> splitText(String text) {
        List<int[]> segments = createSegments(text);
        mergeShortTail(text, segments);

        List<String> chunkList = new ArrayList<>();
        for (int[] segment : segments) {
            String chunk = text.substring(segment[2], segment[1]).strip();
            if (!chunk.isEmpty()) {
                chunkList.add(chunk);
            }
        }
        return chunkList;
    }

    private List<int[]> createSegments(String text) {
        List<int[]> segments = new ArrayList<>();
        int length = text.length();
        int start = skipWhitespace(text, 0);
        int previousStart = -1;
        while (start < length) {
            int base = start;
            if (previousStart >= 0 && chunkOverlap > 0) {
                base = findOverlapStart(text, Math.max(previousStart, start - chunkOverlap), start);
            }
            int end = (length - base <= chunkSize) ? length : findCutIndex(text, base, start);
            segments.add(new int[]{start, end, base});
            previousStart = start;
            start = skipWhitespace(text, end);
        }
        return segments;
    }

    private int findCutIndex(String text, int base, int start) {
        int max = base + chunkSize;
        int min = Math.max(base + minChunkSize, start + 1);
        for (String separator : CUT_SEPARATORS) {
            int index = text.lastIndexOf(separator, max - separator.length());
            if (index >= min) {
                return Character.isWhitespace(separator.charAt(0)) ? index : index + 1;
            }
        }
        return Character.isLowSurrogate(text.charAt(max)) ? max - 1 : max;
    }

    private void mergeShortTail(String text, List<int[]> segments) {
        if (segments.size() <= 1) {
            return;
        }
        int[] last = segments.get(segments.size() - 1);
        if (text.substring(last[0], last[1]).strip().length() < minTailSize) {
            segments.remove(segments.size() - 1);
            segments.get(segments.size() - 1)[1] = last[1];
        }
    }

    private int findOverlapStart(String text, int from, int segmentStart) {
        int best = -1;
        for (String separator : SENTENCE_SEPARATORS) {
            int index = text.indexOf(separator, from);
            int candidate = index + separator.length();
            if (index >= 0 && candidate < segmentStart && (best < 0 || candidate < best)) {
                best = candidate;
            }
        }
        if (best < 0) {
            int space = text.indexOf(' ', from);
            if (space >= 0 && space + 1 < segmentStart) {
                best = space + 1;
            }
        }
        if (best < 0) {
            best = Character.isLowSurrogate(text.charAt(from)) ? from + 1 : from;
        }
        return best;
    }

    private int skipWhitespace(String text, int index) {
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }
}
