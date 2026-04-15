package com.luisa.wechat.chat.model.dto;

public record ConversationSummary(
        String type,
        String id,
        String title,
        String subtitle,
        String avatarUrl,
        int unreadCount
) {
}
