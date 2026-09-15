package com.miraeasset.elibrary.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable pagination envelope for the public API.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {

    public static <T> PageResponse<T> from(Page<T> source) {
        return new PageResponse<>(
                source.getContent(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isLast());
    }
}
