package inha.gdgoc.global.dto.response;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

public record PageMeta(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious,
        String sort,
        String direction
) {

    public static PageMeta of(Page<?> page) {
        String sortProps = page.getSort().stream()
                .map(Sort.Order::getProperty)
                .reduce((a, b) -> a + "," + b)
                .orElse("createdAt");

        String dir = page.getSort().stream()
                .findFirst()
                .map(o -> o.getDirection().name())
                .orElse("DESC");

        return new PageMeta(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious(),
                sortProps,
                dir
        );
    }
}
