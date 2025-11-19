package org.quizly.quizly.core.util;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TextProcessingUtil {

  public static List<String> createChunkList(String text, int chunkSize, int chunkOverlap) {
    List<String> chunkList = new ArrayList<>();

    if (text == null || text.isEmpty()) {
      return chunkList;
    }

    if (text.length() <= chunkSize) {
      chunkList.add(text);
      return chunkList;
    }

    int start = 0;
    while (start < text.length()) {
      int end = Math.min(start + chunkSize, text.length());
      chunkList.add(text.substring(start, end));
      start += chunkSize - chunkOverlap;
    }

    return chunkList;
  }
}
