package com.yep.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Generic pagination envelope returned by list endpoints: {items, page, size,
 * totalItems, totalPages}.
 */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
