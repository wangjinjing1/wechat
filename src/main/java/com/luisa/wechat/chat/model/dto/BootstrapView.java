package com.luisa.wechat.chat.model.dto;

import java.util.List;

public record BootstrapView(
        UserProfile me,
        List<UserSummary> users,
        List<UserSummary> friends,
        List<FriendRequestView> incomingFriendRequests,
        List<GroupSummary> groups,
        int lobbyUnread
) {
}
