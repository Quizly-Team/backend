package org.quizly.quizly.core.application;

import java.util.concurrent.CompletableFuture;

public interface BaseAsyncService<Q extends BaseRequest, R extends BaseResponse> {

  CompletableFuture<R> execute(Q request);
}

