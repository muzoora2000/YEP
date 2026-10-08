package com.yep.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Uniform, bounded pagination envelope for every list endpoint
 * (keeps responses small on low-bandwidth connections).
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }

    public static <T> PageResponse<T> of(List<T> content) {
        return new PageResponse<>(content, 0, Math.max(content.size(), 1), content.size(), 1, true);
    }
}
