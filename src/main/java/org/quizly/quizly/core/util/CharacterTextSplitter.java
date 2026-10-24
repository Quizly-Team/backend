package org.quizly.quizly.core.util;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.transformer.splitter.TextSplitter;

/**
 * 문단 → 줄 → 문장 → 단어 경계 순으로 텍스트를 글자 단위 청크로 나눈다.
 * <p>
 * 모든 경계를 원문 index 기준으로 계산하므로 글자 손실이나 단어 중간 절단이 없다. Spring AI의 TokenTextSplitter는
 * 자른 텍스트를 다시 토큰화해 원본 토큰을 제거하는 방식이라 한글에서 글자가 사라지거나 깨지므로 사용하지 않는다.
 */
public class CharacterTextSplitter extends TextSplitter {

    private static final List<String> CUT_SEPARATORS = List.of("\n\n", "\n", ". ", "? ", "! ", " ");
    private static final List<String> SENTENCE_SEPARATORS = List.of("\n", ". ", "? ", "! ");

    private final int chunkSize;
    private final int chunkOverlap;
    // 구분자를 찾기 시작하는 최소 위치. 이보다 앞의 구분자는 사용하지 않는다.
    private final int minChunkSize;
    // 마지막 청크가 이 길이 미만이면 앞 청크에 합친다.
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

    // {본문 시작, 끝, overlap 포함 시작} 목록. overlap을 포함한 길이가 chunkSize를 넘지 않도록
    // overlap 시작을 먼저 정한 뒤 그 위치를 기준으로 자른다. 다음 구간은 앞 구간 끝의 공백을 건너뛴 위치에서 시작한다.
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

    // [base + minChunkSize, base + chunkSize] 안에서 우선순위가 높은 구분자의 마지막 위치를 찾는다.
    // 본문이 비지 않도록 자르는 위치는 항상 본문 시작(start) 뒤다.
    private int findCutIndex(String text, int base, int start) {
        int max = base + chunkSize;
        int min = Math.max(base + minChunkSize, start + 1);
        for (String separator : CUT_SEPARATORS) {
            int index = text.lastIndexOf(separator, max - separator.length());
            if (index >= min) {
                // 공백류는 구분자 앞에서, 문장부호는 부호까지 포함해서 자른다.
                return Character.isWhitespace(separator.charAt(0)) ? index : index + 1;
            }
        }
        // 구분자가 없으면 강제로 자르되, 이모지 같은 서로게이트 쌍은 쪼개지 않는다.
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

    // from 이후 가장 앞쪽 문장 시작점, 없으면 단어 시작점. 둘 다 없으면 overlap 없이 segmentStart를 반환한다.
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
        return best < 0 ? segmentStart : best;
    }

    private int skipWhitespace(String text, int index) {
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }
}
