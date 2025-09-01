package org.quizly.quizly.core.application;

public interface BaseService<Q extends BaseRequest, R extends BaseResponse> {

  R execute(Q request);
}

