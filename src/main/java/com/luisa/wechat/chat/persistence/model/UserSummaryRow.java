package com.luisa.wechat.chat.persistence.model;

public class UserSummaryRow {

    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
    // Whether the current user and this user are already friends.
    private boolean friend;
    // Whether the current user has sent a pending friend request.
    private boolean outgoingRequest;
    // Whether this user has sent a pending friend request to the current user.
    private boolean incomingRequest;
    // Owner-specific remark stored on the friendship relation.
    private String friendRemark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public boolean isFriend() {
        return friend;
    }

    public void setFriend(boolean friend) {
        this.friend = friend;
    }

    public boolean isOutgoingRequest() {
        return outgoingRequest;
    }

    public void setOutgoingRequest(boolean outgoingRequest) {
        this.outgoingRequest = outgoingRequest;
    }

    public boolean isIncomingRequest() {
        return incomingRequest;
    }

    public void setIncomingRequest(boolean incomingRequest) {
        this.incomingRequest = incomingRequest;
    }

    public String getFriendRemark() {
        return friendRemark;
    }

    public void setFriendRemark(String friendRemark) {
        this.friendRemark = friendRemark;
    }
}
