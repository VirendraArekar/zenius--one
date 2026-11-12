package com.zenius.one.platform.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import org.springframework.data.domain.Page;

/**
 * Standard API response envelope shared across all Zenius One modules.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        Meta meta
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, new Meta(Instant.now(), null, null, null, null));
    }

    public static <T> ApiResponse<T> page(Page<?> page, T content) {
        Meta meta = new Meta(
                Instant.now(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
        return new ApiResponse<>(true, content, meta);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Meta(
            Instant timestamp,
            Integer page,
            Integer size,
            Long totalElements,
            Integer totalPages
    ) {
    }
}

