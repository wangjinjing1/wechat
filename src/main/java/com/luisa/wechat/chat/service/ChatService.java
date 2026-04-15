package com.luisa.wechat.chat.service;

import com.luisa.wechat.chat.model.dto.BootstrapView;
import com.luisa.wechat.chat.model.dto.ConversationSummary;
import com.luisa.wechat.chat.model.dto.FriendRequestView;
import com.luisa.wechat.chat.model.dto.GroupMemberView;
import com.luisa.wechat.chat.model.dto.GroupSummary;
import com.luisa.wechat.chat.model.dto.MessagePage;
import com.luisa.wechat.chat.model.dto.MessageView;
import com.luisa.wechat.chat.model.dto.PagedResult;
import com.luisa.wechat.chat.model.dto.UserProfile;
import com.luisa.wechat.chat.model.dto.UserSummary;
import com.luisa.wechat.chat.model.entity.UserRecord;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ChatService {

    UserProfile register(String username, String displayName, String password);

    UserRecord authenticate(String username, String password);

    UserRecord requireUser(String username);

    void changePassword(String username, String currentPassword, String newPassword, String confirmPassword);

    BootstrapView bootstrap(String username);

    PagedResult<ConversationSummary> listConversations(String username, String keyword, Integer page, Integer size);

    PagedResult<UserSummary> listUsers(String username, String keyword, Integer page, Integer size);

    PagedResult<FriendRequestView> listFriendRequests(String username, String keyword, Integer page, Integer size);

    void addFriend(String username, String targetUsername);

    UserSummary updateFriendRemark(String username, String targetUsername, String friendRemark);

    void removeFriend(String username, String targetUsername);

    void acceptFriendRequest(String username, Long requestId);

    void rejectFriendRequest(String username, Long requestId);

    GroupSummary createGroup(String username, String groupName, List<String> memberUsernames);

    GroupSummary getGroup(String username, Long groupId);

    GroupSummary addGroupMember(String username, Long groupId, String memberUsername);

    PagedResult<GroupMemberView> listGroupMembers(String username, Long groupId, String keyword, Integer page, Integer size);

    GroupSummary removeGroupMember(String username, Long groupId, Long memberUserId);

    UserProfile updateAvatar(String username, MultipartFile file);

    MessagePage listLobbyMessages(String username, Long beforeId);

    MessageView sendLobbyMessage(String username, String type, String content);

    MessageView sendLobbyFile(String username, MultipartFile file);

    MessagePage listDirectMessages(String username, String targetUsername, Long beforeId);

    MessageView sendDirectMessage(String username, String targetUsername, String type, String content);

    MessageView sendDirectFile(String username, String targetUsername, MultipartFile file);

    MessagePage listGroupMessages(String username, Long groupId, Long beforeId);

    MessageView sendGroupMessage(String username, Long groupId, String type, String content);

    MessageView sendGroupFile(String username, Long groupId, MultipartFile file);
}
