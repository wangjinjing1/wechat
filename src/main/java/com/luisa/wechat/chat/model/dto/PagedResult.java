package com.luisa.wechat.chat.model.dto;

import java.util.List;

public record PagedResult<T>(
        List<T> items,
        int page,
        int size,
        long total,
        boolean hasMore
) {
}
