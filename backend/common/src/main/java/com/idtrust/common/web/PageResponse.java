package com.idtrust.common.web;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Standard paginated-list response envelope used across every IDTrust list endpoint, per the
 * IDTrust_v1.2 Hardened API Specification (which defines a dedicated {@code *Page} wrapper schema
 * - e.g. {@code OrganizationPage}/{@code TenantPage}/{@code UserPage} - for every collection
 * resource instead of returning a bare JSON array). Every list endpoint that previously returned
 * {@code List<T>} directly should return {@code PageResponse<T>} instead, so clients can page
 * reliably and know the total result count up front.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
        return of(page.map(mapper));
    }
}
