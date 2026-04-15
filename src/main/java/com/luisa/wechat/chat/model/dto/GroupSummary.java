package com.luisa.wechat.chat.model.dto;

import java.util.List;

public record GroupSummary(
        Long id,
        String name,
        String avatarUrl,
        Long ownerId,
        int unreadCount,
        String myRole,
        List<GroupMemberView> members
) {
}
