package com.luisa.wechat.chat.model.dto;

import java.util.List;

public record MessagePage(List<MessageView> items, boolean hasMore, Long nextBeforeId) {
}
