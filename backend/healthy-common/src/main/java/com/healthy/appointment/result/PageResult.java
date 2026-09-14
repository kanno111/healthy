package com.healthy.appointment.result;

import java.util.List;

/** 通用分页查询结果。 */
public record PageResult<T>(
        List<T> records,
        long total,
        int page,
        int pageSize,
        long totalPages
) {
    public static <T> PageResult<T> of(List<T> records, long total, int page, int pageSize) {
        long totalPages = (total + pageSize - 1) / pageSize;
        return new PageResult<>(records, total, page, pageSize, totalPages);
    }
}
