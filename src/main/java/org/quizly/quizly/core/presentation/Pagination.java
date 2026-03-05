package org.quizly.quizly.core.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "페이지네이션 응답")
public class Pagination {

    @Schema(description = "현재 페이지", example = "1")
    private int page;

    @Schema(description = "페이지 크기", example = "10")
    private int pageSize;

    @Schema(description = "전체 페이지 수", example = "5")
    private int totalPages;

    @Schema(description = "전체 요소 수", example = "50")
    private long totalElements;

    public static Pagination getPaginationFromPage(Page<?> page) {
        return Pagination.builder()
            .page(page.getNumber() + 1)
            .pageSize(page.getSize())
            .totalPages(page.getTotalPages())
            .totalElements(page.getTotalElements())
            .build();
    }
}
