package com.realestate.shared.application;

import java.util.List;

public record PageResult<T>(List<T> items, int page, int size, long totalElements) {
    public PageResult {
        items = List.copyOf(items);
    }

    public long totalPages() {
        return (totalElements + size - 1) / size;
    }
}
