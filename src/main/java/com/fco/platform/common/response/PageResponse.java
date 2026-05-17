package com.fco.platform.common.response;

import java.util.List;

public class PageResponse<T> extends PaginationResponse<T> {
    public PageResponse() {
        super();
    }

    public PageResponse(List<T> content, int page, int size, long totalElements, int totalPages) {
        super(content, page, size, totalElements, totalPages);
    }
}
