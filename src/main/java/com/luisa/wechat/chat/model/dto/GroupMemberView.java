package com.luisa.wechat.chat.model.dto;

public record GroupMemberView(Long userId, String username, String displayName, String avatarUrl, String role) {
}
