package org.quizly.quizly.core.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.quizly.quizly.core.application.BaseRequest;
import org.springframework.data.domain.PageRequest;

@Getter
@Setter
@Schema(description = "페이지네이션 요청 공통 기반")
public class BasePaginationRequest implements BaseRequest {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    @Schema(description = "페이지", example = "1")
    protected Integer page;

    @Schema(description = "페이지 사이즈", example = "10")
    protected Integer pageSize;

    public PageRequest toPageRequest() {
        int validatedPage = (page == null || page < 1) ? DEFAULT_PAGE : page;
        int validatedPageSize = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE
            : Math.min(pageSize, MAX_PAGE_SIZE);
        return PageRequest.of(validatedPage - 1, validatedPageSize);
    }
}
