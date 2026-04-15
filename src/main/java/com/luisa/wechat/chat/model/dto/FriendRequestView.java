package com.luisa.wechat.chat.model.dto;

import java.time.Instant;

public record FriendRequestView(
        Long id,
        Long requesterId,
        String requesterUsername,
        String requesterDisplayName,
        String requesterAvatarUrl,
        Instant createdAt
) {
}
