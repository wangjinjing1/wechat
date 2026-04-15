package com.luisa.wechat.chat.model.dto;

import java.time.Instant;

public record MessageView(
        Long id,
        Long senderId,
        String senderName,
        String senderAvatarUrl,
        String contentType,
        String content,
        String fileName,
        String fileUrl,
        Instant createdAt
) {
}
