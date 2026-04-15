package com.luisa.wechat.chat.model.dto;

public record UserSummary(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        String friendRemark,
        boolean friend,
        boolean outgoingRequest,
        boolean incomingRequest,
        int unreadCount
) {
}
