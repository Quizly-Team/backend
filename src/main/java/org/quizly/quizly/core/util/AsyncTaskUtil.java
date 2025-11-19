package org.quizly.quizly.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

@Log4j2
@UtilityClass
public class AsyncTaskUtil {

  public static <T> List<CompletableFuture<T>> requestAsyncTasks(
      List<String> chunkList,
      int totalCount,
      int batchSize,
      BiFunction<String, Integer, CompletableFuture<T>> taskCreator,
      String serviceName) {

    Collections.shuffle(chunkList);
    List<CompletableFuture<T>> futures = new ArrayList<>();
    int totalTasks = (totalCount + batchSize - 1) / batchSize;
    int chunkListSize = chunkList.size();

    for (int i = 0; i < totalTasks; i++) {
      String selectedChunk = chunkList.get(i % chunkListSize);
      CompletableFuture<T> future = taskCreator.apply(selectedChunk, batchSize);

      futures.add(future.exceptionally(ex -> {
        log.error("[{}] Async task failed for chunk (length: {})", serviceName, selectedChunk.length(), ex);
        return null;
      }));
    }

    return futures;
  }

  public static <Response, Item> List<Item> joinAsyncTasks(
      List<CompletableFuture<Response>> futures,
      java.util.function.Function<Response, List<Item>> responseExtractor) {

    return futures.stream()
        .map(CompletableFuture::join)
        .filter(Objects::nonNull)
        .map(responseExtractor)
        .filter(Objects::nonNull)
        .flatMap(List::stream)
        .toList();
  }
}
