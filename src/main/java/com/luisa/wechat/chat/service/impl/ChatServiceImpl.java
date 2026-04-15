package com.luisa.wechat.chat.service.impl;

import com.luisa.wechat.chat.mapper.ConversationStateMapper;
import com.luisa.wechat.chat.mapper.FriendRequestMapper;
import com.luisa.wechat.chat.mapper.FriendshipMapper;
import com.luisa.wechat.chat.mapper.GroupMapper;
import com.luisa.wechat.chat.mapper.GroupMemberMapper;
import com.luisa.wechat.chat.mapper.MessageMapper;
import com.luisa.wechat.chat.mapper.UserMapper;
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
import com.luisa.wechat.chat.model.entity.ChatGroupRecord;
import com.luisa.wechat.chat.model.entity.ChatMessageRecord;
import com.luisa.wechat.chat.model.entity.ConversationStateRecord;
import com.luisa.wechat.chat.model.entity.FriendRequestRow;
import com.luisa.wechat.chat.model.entity.GroupMemberRecord;
import com.luisa.wechat.chat.model.entity.GroupSummaryRow;
import com.luisa.wechat.chat.model.entity.MessageRow;
import com.luisa.wechat.chat.model.entity.UserRecord;
import com.luisa.wechat.chat.model.entity.UserSummaryRow;
import com.luisa.wechat.chat.config.exception.ChatException;
import com.luisa.wechat.chat.service.ChatService;
import com.luisa.wechat.chat.config.exception.UnauthorizedException;
import com.luisa.wechat.chat.config.ChatWebSocketNotifier;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Default implementation that coordinates chat domain operations.
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final String LOBBY_KEY = "lobby";
    private static final int MESSAGE_PAGE_SIZE = 10;
    private static final int DEFAULT_SIDEBAR_PAGE_SIZE = 10;
    private static final int MAX_SIDEBAR_PAGE_SIZE = 50;

    private final UserMapper userMapper;
    private final FriendRequestMapper friendRequestMapper;
    private final FriendshipMapper friendshipMapper;
    private final GroupMapper groupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final MessageMapper messageMapper;
    private final ConversationStateMapper conversationStateMapper;
    private final PasswordEncoder passwordEncoder;
    private final ChatWebSocketNotifier notifier;
    private final Path uploadDir;
    private final Path avatarUploadDir;
    private final Path chatUploadDir;

    public ChatServiceImpl(UserMapper userMapper,
                       FriendRequestMapper friendRequestMapper,
                       FriendshipMapper friendshipMapper,
                       GroupMapper groupMapper,
                       GroupMemberMapper groupMemberMapper,
                       MessageMapper messageMapper,
                       ConversationStateMapper conversationStateMapper,
                       PasswordEncoder passwordEncoder,
                       ChatWebSocketNotifier notifier,
                       @Value("${app.storage.upload-dir}") String uploadDir) {
        this.userMapper = userMapper;
        this.friendRequestMapper = friendRequestMapper;
        this.friendshipMapper = friendshipMapper;
        this.groupMapper = groupMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.messageMapper = messageMapper;
        this.conversationStateMapper = conversationStateMapper;
        this.passwordEncoder = passwordEncoder;
        this.notifier = notifier;
        this.uploadDir = Path.of(uploadDir);
        this.avatarUploadDir = this.uploadDir.resolve("avatar");
        this.chatUploadDir = this.uploadDir.resolve("chat");
        try {
            Files.createDirectories(this.avatarUploadDir);
            Files.createDirectories(this.chatUploadDir);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create upload directory", ex);
        }
    }

    @Transactional
    public UserProfile register(String username, String displayName, String password) {
        String normalizedUsername = normalizeUsername(username);
        validatePassword(password);
        if (userMapper.findByUsername(normalizedUsername) != null) {
            throw new ChatException("用户名已存在");
        }
        UserRecord user = new UserRecord();
        user.setUsername(normalizedUsername);
        user.setDisplayName(normalizeDisplayName(displayName, normalizedUsername));
        user.setPasswordHash(passwordEncoder.encode(password.trim()));
        user.setCreatedAt(Instant.now());
        userMapper.insert(user);
        return toProfile(userMapper.findById(user.getId()));
    }

    public UserRecord authenticate(String username, String password) {
        UserRecord user = userMapper.findByUsername(normalizeUsername(username));
        if (user == null) {
            throw new ChatException("用户不存在");
        }
        if (!passwordEncoder.matches(Objects.requireNonNullElse(password, ""), user.getPasswordHash())) {
            throw new ChatException("密码错误");
        }
        return user;
    }

    public UserRecord requireUser(String username) {
        UserRecord user = userMapper.findByUsername(normalizeUsername(username));
        if (user == null) {
            throw new UnauthorizedException("请先登录");
        }
        return user;
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword, String confirmPassword) {
        UserRecord user = requireUser(username);
        if (!passwordEncoder.matches(Objects.requireNonNullElse(currentPassword, ""), user.getPasswordHash())) {
            throw new ChatException("当前密码错误");
        }
        validatePassword(newPassword);
        String normalizedNewPassword = Objects.requireNonNullElse(newPassword, "").trim();
        if (!normalizedNewPassword.equals(Objects.requireNonNullElse(confirmPassword, "").trim())) {
            throw new ChatException("两次输入的新密码不一致");
        }
        if (passwordEncoder.matches(normalizedNewPassword, user.getPasswordHash())) {
            throw new ChatException("新密码不能和当前密码相同");
        }
        userMapper.updatePasswordHash(user.getId(), passwordEncoder.encode(normalizedNewPassword));
    }

    @Transactional(readOnly = true)
    public BootstrapView bootstrap(String username) {
        UserRecord currentUser = requireUser(username);
        List<UserSummary> users = userMapper.listUsers(currentUser.getId()).stream()
                .map(row -> toUserSummary(currentUser.getId(), row))
                .toList();
        List<UserSummary> friends = friendshipMapper.listFriends(currentUser.getId()).stream()
                .map(row -> toUserSummary(currentUser.getId(), row))
                .toList();
        List<FriendRequestView> incomingFriendRequests = friendRequestMapper.listIncoming(currentUser.getId()).stream()
                .map(row -> new FriendRequestView(
                        row.getId(),
                        row.getRequesterId(),
                        row.getRequesterUsername(),
                        row.getRequesterDisplayName(),
                        row.getRequesterAvatarUrl(),
                        row.getCreatedAt()
                ))
                .toList();
        List<GroupSummary> groups = groupMapper.listGroupsForUser(currentUser.getId()).stream()
                .map(row -> toGroupSummary(currentUser.getId(), row))
                .toList();
        int lobbyUnread = unreadCount(currentUser.getId(), LOBBY_KEY);
        return new BootstrapView(
                toProfile(currentUser),
                users,
                friends,
                incomingFriendRequests,
                groups,
                lobbyUnread
        );
    }

    @Transactional(readOnly = true)
    public PagedResult<ConversationSummary> listConversations(String username, String keyword, Integer page, Integer size) {
        UserRecord currentUser = requireUser(username);
        List<ConversationSummary> items = new ArrayList<>();
        items.add(new ConversationSummary("lobby", "lobby", "聊天大厅", "所有用户都可见", null, unreadCount(currentUser.getId(), LOBBY_KEY)));
        friendshipMapper.listFriends(currentUser.getId()).forEach(row -> {
            UserSummary summary = toUserSummary(currentUser.getId(), row);
            items.add(new ConversationSummary(
                    "direct",
                    summary.username(),
                    Objects.requireNonNullElse(summary.friendRemark(), summary.displayName()),
                    "好友 @" + summary.username(),
                    summary.avatarUrl(),
                    summary.unreadCount()
            ));
        });
        groupMapper.listGroupsForUser(currentUser.getId()).forEach(row -> {
            GroupSummary summary = toGroupSummary(currentUser.getId(), row);
            items.add(new ConversationSummary(
                    "group",
                    String.valueOf(summary.id()),
                    summary.name(),
                    summary.members().size() + " 位成员",
                    summary.avatarUrl(),
                    summary.unreadCount()
            ));
        });
        List<ConversationSummary> filtered = items.stream()
                .filter(item -> matchesKeyword(keyword, item.title(), item.subtitle(), item.id()))
                .toList();
        return paginate(filtered, page, size);
    }

    @Transactional(readOnly = true)
    public PagedResult<UserSummary> listUsers(String username, String keyword, Integer page, Integer size) {
        UserRecord currentUser = requireUser(username);
        List<UserSummary> filtered = userMapper.listUsers(currentUser.getId()).stream()
                .map(row -> toUserSummary(currentUser.getId(), row))
                .filter(item -> matchesKeyword(keyword, item.displayName(), item.username(), item.friendRemark()))
                .toList();
        return paginate(filtered, page, size);
    }

    @Transactional(readOnly = true)
    public PagedResult<FriendRequestView> listFriendRequests(String username, String keyword, Integer page, Integer size) {
        UserRecord currentUser = requireUser(username);
        List<FriendRequestView> filtered = friendRequestMapper.listIncoming(currentUser.getId()).stream()
                .map(row -> new FriendRequestView(
                        row.getId(),
                        row.getRequesterId(),
                        row.getRequesterUsername(),
                        row.getRequesterDisplayName(),
                        row.getRequesterAvatarUrl(),
                        row.getCreatedAt()
                ))
                .filter(item -> matchesKeyword(keyword, item.requesterDisplayName(), item.requesterUsername()))
                .toList();
        return paginate(filtered, page, size);
    }

    @Transactional(readOnly = true)
    public PagedResult<GroupMemberView> listGroupMembers(String username, Long groupId, String keyword, Integer page, Integer size) {
        UserRecord currentUser = requireUser(username);
        requireGroupMember(currentUser.getId(), groupId);
        List<GroupMemberView> filtered = groupMemberMapper.listMembers(groupId).stream()
                .map(row -> new GroupMemberView(row.getUserId(), row.getUsername(), row.getDisplayName(), row.getAvatarUrl(), row.getRole()))
                .filter(item -> matchesKeyword(keyword, item.displayName(), item.username(), item.role()))
                .toList();
        return paginate(filtered, page, size);
    }

    @Transactional
    public void addFriend(String username, String targetUsername) {
        UserRecord currentUser = requireUser(username);
        UserRecord target = findUserByUsername(targetUsername);
        if (currentUser.getId().equals(target.getId())) {
            throw new ChatException("不能添加自己为好友");
        }
        if (friendshipMapper.countRelation(currentUser.getId(), target.getId()) > 0) {
            throw new ChatException("已经是好友了");
        }

        FriendRequestRow reverseRequest = friendRequestMapper.findByRequesterAndRecipient(target.getId(), currentUser.getId());
        if (reverseRequest != null) {
            acceptFriendRequestInternal(currentUser, target, reverseRequest);
            notifier.notifyUsers(Set.of(currentUser.getUsername(), target.getUsername()));
            return;
        }

        if (friendRequestMapper.findByRequesterAndRecipient(currentUser.getId(), target.getId()) != null) {
            throw new ChatException("好友申请已发送，等待对方同意");
        }

        FriendRequestRow request = new FriendRequestRow();
        request.setRequesterId(currentUser.getId());
        request.setRecipientId(target.getId());
        request.setCreatedAt(Instant.now());
        try {
            friendRequestMapper.insert(request);
        } catch (DuplicateKeyException ignored) {
        }
        notifier.notifyUsers(Set.of(currentUser.getUsername(), target.getUsername()));
    }

    @Transactional
    public UserSummary updateFriendRemark(String username, String targetUsername, String friendRemark) {
        UserRecord currentUser = requireUser(username);
        UserRecord target = findUserByUsername(targetUsername);
        ensureFriends(currentUser.getId(), target.getId());
        String normalizedRemark = normalizeFriendRemark(friendRemark);
        friendshipMapper.updateRemark(currentUser.getId(), target.getId(), normalizedRemark);
        UserSummaryRow updated = friendshipMapper.listFriends(currentUser.getId()).stream()
                .filter(row -> Objects.equals(row.getId(), target.getId()))
                .findFirst()
                .orElseThrow(() -> new ChatException("好友不存在"));
        return toUserSummary(currentUser.getId(), updated);
    }

    @Transactional
    public void removeFriend(String username, String targetUsername) {
        UserRecord currentUser = requireUser(username);
        UserRecord target = findUserByUsername(targetUsername);
        ensureFriends(currentUser.getId(), target.getId());
        friendshipMapper.deletePair(currentUser.getId(), target.getId());
        friendRequestMapper.deleteByRequesterAndRecipient(currentUser.getId(), target.getId());
        friendRequestMapper.deleteByRequesterAndRecipient(target.getId(), currentUser.getId());
        notifier.notifyUsers(Set.of(currentUser.getUsername(), target.getUsername()));
    }

    @Transactional
    public void acceptFriendRequest(String username, Long requestId) {
        UserRecord currentUser = requireUser(username);
        FriendRequestRow request = requireIncomingFriendRequest(currentUser.getId(), requestId);
        UserRecord requester = requireUserById(request.getRequesterId());
        acceptFriendRequestInternal(currentUser, requester, request);
        notifier.notifyUsers(Set.of(currentUser.getUsername(), requester.getUsername()));
    }

    @Transactional
    public void rejectFriendRequest(String username, Long requestId) {
        UserRecord currentUser = requireUser(username);
        FriendRequestRow request = requireIncomingFriendRequest(currentUser.getId(), requestId);
        friendRequestMapper.deleteById(requestId);
        UserRecord requester = requireUserById(request.getRequesterId());
        notifier.notifyUsers(Set.of(currentUser.getUsername(), requester.getUsername()));
    }

    @Transactional
    public GroupSummary createGroup(String username, String groupName, List<String> memberUsernames) {
        UserRecord owner = requireUser(username);
        String normalizedGroupName = Objects.requireNonNullElse(groupName, "").trim();
        if (normalizedGroupName.isBlank()) {
            throw new ChatException("群名称不能为空");
        }
        ChatGroupRecord group = new ChatGroupRecord();
        group.setName(normalizedGroupName);
        group.setOwnerId(owner.getId());
        group.setCreatedAt(Instant.now());
        groupMapper.insert(group);

        addGroupMemberRecord(group.getId(), owner.getId(), "OWNER");
        if (memberUsernames != null) {
            for (String memberUsername : memberUsernames) {
                UserRecord user = findUserByUsername(memberUsername);
                if (!user.getId().equals(owner.getId()) && groupMemberMapper.findByGroupAndUser(group.getId(), user.getId()) == null) {
                    addGroupMemberRecord(group.getId(), user.getId(), "MEMBER");
                }
            }
        }
        notifier.notifyUsers(usernamesOfGroup(group.getId()));
        return getGroup(username, group.getId());
    }

    @Transactional(readOnly = true)
    public GroupSummary getGroup(String username, Long groupId) {
        UserRecord currentUser = requireUser(username);
        GroupMemberRecord membership = requireGroupMember(currentUser.getId(), groupId);
        ChatGroupRecord group = requireGroup(groupId);
        return buildGroupSummary(currentUser.getId(), group, membership.getRole());
    }

    @Transactional
    public GroupSummary addGroupMember(String username, Long groupId, String memberUsername) {
        UserRecord currentUser = requireUser(username);
        ChatGroupRecord group = requireGroup(groupId);
        if (!group.getOwnerId().equals(currentUser.getId())) {
            throw new ChatException("只有群主可以添加成员");
        }
        UserRecord member = findUserByUsername(memberUsername);
        if (groupMemberMapper.findByGroupAndUser(groupId, member.getId()) == null) {
            addGroupMemberRecord(groupId, member.getId(), "MEMBER");
        }
        notifier.notifyUsers(usernamesOfGroup(groupId));
        return buildGroupSummary(currentUser.getId(), group, "OWNER");
    }

    @Transactional
    public GroupSummary removeGroupMember(String username, Long groupId, Long memberUserId) {
        UserRecord currentUser = requireUser(username);
        ChatGroupRecord group = requireGroup(groupId);
        if (!group.getOwnerId().equals(currentUser.getId())) {
            throw new ChatException("只有群主可以移除成员");
        }
        if (group.getOwnerId().equals(memberUserId)) {
            throw new ChatException("不能移除群主");
        }
        groupMemberMapper.delete(groupId, memberUserId);
        notifier.notifyUsers(usernamesOfGroup(groupId));
        UserRecord removed = userMapper.findById(memberUserId);
        if (removed != null) {
            notifier.notifyUsers(Set.of(removed.getUsername()));
        }
        return buildGroupSummary(currentUser.getId(), group, "OWNER");
    }

    @Transactional
    public MessagePage listLobbyMessages(String username, Long beforeId) {
        UserRecord currentUser = requireUser(username);
        return listConversationMessages(currentUser.getId(), LOBBY_KEY, beforeId);
    }

    @Transactional
    public MessagePage listDirectMessages(String username, String targetUsername, Long beforeId) {
        UserRecord currentUser = requireUser(username);
        UserRecord target = findUserByUsername(targetUsername);
        ensureFriends(currentUser.getId(), target.getId());
        return listConversationMessages(currentUser.getId(), directConversationKey(currentUser.getId(), target.getId()), beforeId);
    }

    @Transactional
    public MessagePage listGroupMessages(String username, Long groupId, Long beforeId) {
        UserRecord currentUser = requireUser(username);
        requireGroupMember(currentUser.getId(), groupId);
        return listConversationMessages(currentUser.getId(), groupConversationKey(groupId), beforeId);
    }

    @Transactional
    public MessageView sendLobbyMessage(String username, String type, String content) {
        UserRecord currentUser = requireUser(username);
        MessageView message = saveMessage(currentUser, LOBBY_KEY, "LOBBY", null, type, content, null, null);
        notifier.notifyLobbyMessage(allKnownUsernames(), message);
        return message;
    }

    @Transactional
    public MessageView sendDirectMessage(String username, String targetUsername, String type, String content) {
        UserRecord currentUser = requireUser(username);
        UserRecord target = findUserByUsername(targetUsername);
        ensureFriends(currentUser.getId(), target.getId());
        MessageView message = saveMessage(
                currentUser,
                directConversationKey(currentUser.getId(), target.getId()),
                "DIRECT",
                null,
                type,
                content,
                null,
                null
        );
        notifier.notifyDirectMessage(Set.of(currentUser.getUsername(), target.getUsername()),
                currentUser.getUsername(), target.getUsername(), message);
        return message;
    }

    @Transactional
    public MessageView sendGroupMessage(String username, Long groupId, String type, String content) {
        UserRecord currentUser = requireUser(username);
        requireGroupMember(currentUser.getId(), groupId);
        MessageView message = saveMessage(currentUser, groupConversationKey(groupId), "GROUP", groupId, type, content, null, null);
        notifier.notifyGroupMessage(usernamesOfGroup(groupId), groupId, message);
        return message;
    }

    @Transactional
    public MessageView sendLobbyFile(String username, MultipartFile file) {
        UserRecord currentUser = requireUser(username);
        StoredFile storedFile = storeFile(file, "chat");
        MessageView message = saveMessage(currentUser, LOBBY_KEY, "LOBBY", null, "FILE",
                storedFile.originalName(), storedFile.originalName(), storedFile.url());
        notifier.notifyLobbyMessage(allKnownUsernames(), message);
        return message;
    }

    @Transactional
    public MessageView sendDirectFile(String username, String targetUsername, MultipartFile file) {
        UserRecord currentUser = requireUser(username);
        UserRecord target = findUserByUsername(targetUsername);
        ensureFriends(currentUser.getId(), target.getId());
        StoredFile storedFile = storeFile(file, "chat");
        MessageView message = saveMessage(
                currentUser,
                directConversationKey(currentUser.getId(), target.getId()),
                "DIRECT",
                null,
                "FILE",
                storedFile.originalName(),
                storedFile.originalName(),
                storedFile.url()
        );
        notifier.notifyDirectMessage(Set.of(currentUser.getUsername(), target.getUsername()),
                currentUser.getUsername(), target.getUsername(), message);
        return message;
    }

    @Transactional
    public MessageView sendGroupFile(String username, Long groupId, MultipartFile file) {
        UserRecord currentUser = requireUser(username);
        requireGroupMember(currentUser.getId(), groupId);
        StoredFile storedFile = storeFile(file, "chat");
        MessageView message = saveMessage(currentUser, groupConversationKey(groupId), "GROUP", groupId, "FILE",
                storedFile.originalName(), storedFile.originalName(), storedFile.url());
        notifier.notifyGroupMessage(usernamesOfGroup(groupId), groupId, message);
        return message;
    }

    @Transactional
    public UserProfile updateAvatar(String username, MultipartFile file) {
        UserRecord currentUser = requireUser(username);
        StoredFile storedFile = storeFile(file, "avatar");
        userMapper.updateAvatar(currentUser.getId(), storedFile.url());
        UserProfile profile = toProfile(userMapper.findById(currentUser.getId()));
        notifier.notifyUsers(allKnownUsernames());
        return profile;
    }

    private void acceptFriendRequestInternal(UserRecord currentUser, UserRecord requester, FriendRequestRow request) {
        createFriendRelationIfMissing(currentUser.getId(), requester.getId());
        createFriendRelationIfMissing(requester.getId(), currentUser.getId());
        friendRequestMapper.deleteById(request.getId());
        friendRequestMapper.deleteByRequesterAndRecipient(currentUser.getId(), requester.getId());
    }

    private void createFriendRelationIfMissing(Long ownerId, Long friendId) {
        if (friendshipMapper.countRelation(ownerId, friendId) == 0) {
            try {
                friendshipMapper.insert(ownerId, friendId, Instant.now());
            } catch (DuplicateKeyException ignored) {
            }
        }
    }

    private void addGroupMemberRecord(Long groupId, Long userId, String role) {
        GroupMemberRecord member = new GroupMemberRecord();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(role);
        member.setJoinedAt(Instant.now());
        groupMemberMapper.insert(member);
    }

    private GroupSummary buildGroupSummary(Long currentUserId, ChatGroupRecord group, String currentRole) {
        List<GroupMemberView> members = groupMemberMapper.listMembers(group.getId()).stream()
                .map(row -> new GroupMemberView(row.getUserId(), row.getUsername(), row.getDisplayName(), row.getAvatarUrl(), row.getRole()))
                .toList();
        int unread = unreadCount(currentUserId, groupConversationKey(group.getId()));
        return new GroupSummary(group.getId(), group.getName(), group.getAvatarUrl(), group.getOwnerId(), unread, currentRole, members);
    }

    private List<MessageView> mapMessages(List<MessageRow> rows) {
        return rows.stream()
                .map(row -> new MessageView(
                        row.getId(),
                        row.getSenderId(),
                        row.getSenderName(),
                        row.getSenderAvatarUrl(),
                        row.getContentType(),
                        row.getContent(),
                        row.getFileName(),
                        row.getFileUrl(),
                        row.getCreatedAt()
                ))
                .toList();
    }

    private MessagePage listConversationMessages(Long currentUserId, String conversationKey, Long beforeId) {
        List<MessageView> messages = mapMessages(messageMapper.listMessages(conversationKey, beforeId, MESSAGE_PAGE_SIZE + 1));
        boolean hasMore = messages.size() > MESSAGE_PAGE_SIZE;
        if (hasMore) {
            messages = messages.subList(1, messages.size());
        }
        if (beforeId == null) {
            markRead(currentUserId, conversationKey);
        }
        Long nextBeforeId = messages.isEmpty() ? null : messages.get(0).id();
        return new MessagePage(messages, hasMore, nextBeforeId);
    }

    private MessageView saveMessage(UserRecord currentUser, String conversationKey, String scope, Long groupId,
                                    String type, String content, String fileName, String fileUrl) {
        String normalizedType = normalizeType(type);
        String normalizedContent = Objects.requireNonNullElse(content, "").trim();
        if (!"FILE".equals(normalizedType) && normalizedContent.isBlank()) {
            throw new ChatException("消息内容不能为空");
        }
        ChatMessageRecord record = new ChatMessageRecord();
        record.setConversationKey(conversationKey);
        record.setScope(scope);
        record.setSenderId(currentUser.getId());
        record.setGroupId(groupId);
        record.setContentType(normalizedType);
        record.setContent(normalizedContent);
        record.setFileName(fileName);
        record.setFileUrl(fileUrl);
        record.setCreatedAt(Instant.now());
        messageMapper.insert(record);
        return new MessageView(
                record.getId(),
                currentUser.getId(),
                currentUser.getDisplayName(),
                currentUser.getAvatarUrl(),
                normalizedType,
                normalizedContent,
                fileName,
                fileUrl,
                record.getCreatedAt()
        );
    }

    private String normalizeType(String type) {
        String normalized = Objects.requireNonNullElse(type, "TEXT").trim().toUpperCase(Locale.ROOT);
        if (!Set.of("TEXT", "EMOJI", "FILE").contains(normalized)) {
            throw new ChatException("不支持的消息类型");
        }
        return normalized;
    }

    private void ensureFriends(Long currentUserId, Long targetUserId) {
        if (friendshipMapper.countRelation(currentUserId, targetUserId) == 0) {
            throw new ChatException("请先成为好友");
        }
    }

    private GroupMemberRecord requireGroupMember(Long userId, Long groupId) {
        GroupMemberRecord membership = groupMemberMapper.findByGroupAndUser(groupId, userId);
        if (membership == null) {
            throw new ChatException("你不在这个群里");
        }
        return membership;
    }

    private ChatGroupRecord requireGroup(Long groupId) {
        ChatGroupRecord group = groupMapper.findById(groupId);
        if (group == null) {
            throw new ChatException("群聊不存在");
        }
        return group;
    }

    private UserRecord findUserByUsername(String username) {
        UserRecord user = userMapper.findByUsername(normalizeUsername(username));
        if (user == null) {
            throw new ChatException("用户不存在");
        }
        return user;
    }

    private UserRecord requireUserById(Long userId) {
        UserRecord user = userMapper.findById(userId);
        if (user == null) {
            throw new ChatException("用户不存在");
        }
        return user;
    }

    private FriendRequestRow requireIncomingFriendRequest(Long recipientId, Long requestId) {
        return friendRequestMapper.listIncoming(recipientId).stream()
                .filter(request -> Objects.equals(request.getId(), requestId))
                .findFirst()
                .orElseThrow(() -> new ChatException("好友申请不存在"));
    }

    private UserSummary toUserSummary(Long currentUserId, UserSummaryRow row) {
        return new UserSummary(
                row.getId(),
                row.getUsername(),
                row.getDisplayName(),
                row.getAvatarUrl(),
                row.getFriendRemark(),
                row.isFriend(),
                row.isOutgoingRequest(),
                row.isIncomingRequest(),
                row.isFriend() ? unreadCount(currentUserId, directConversationKey(currentUserId, row.getId())) : 0
        );
    }

    private GroupSummary toGroupSummary(Long currentUserId, GroupSummaryRow row) {
        GroupMemberRecord membership = groupMemberMapper.findByGroupAndUser(row.getId(), currentUserId);
        return buildGroupSummary(currentUserId, groupMapper.findById(row.getId()), membership == null ? "MEMBER" : membership.getRole());
    }

    private int unreadCount(Long userId, String conversationKey) {
        ConversationStateRecord state = conversationStateMapper.findByUserAndConversation(userId, conversationKey);
        long lastReadId = state == null ? 0L : Objects.requireNonNullElse(state.getLastReadMessageId(), 0L);
        return messageMapper.countUnread(conversationKey, lastReadId, userId);
    }

    private boolean matchesKeyword(String keyword, String... values) {
        String normalizedKeyword = Objects.requireNonNullElse(keyword, "").trim().toLowerCase(Locale.ROOT);
        if (normalizedKeyword.isBlank()) {
            return true;
        }
        for (String value : values) {
            if (Objects.requireNonNullElse(value, "").toLowerCase(Locale.ROOT).contains(normalizedKeyword)) {
                return true;
            }
        }
        return false;
    }

    private <T> PagedResult<T> paginate(List<T> items, Integer page, Integer size) {
        int normalizedPage = Math.max(0, Objects.requireNonNullElse(page, 0));
        int normalizedSize = Math.min(MAX_SIDEBAR_PAGE_SIZE, Math.max(1, Objects.requireNonNullElse(size, DEFAULT_SIDEBAR_PAGE_SIZE)));
        int fromIndex = Math.min(items.size(), normalizedPage * normalizedSize);
        int toIndex = Math.min(items.size(), fromIndex + normalizedSize);
        List<T> pageItems = items.subList(fromIndex, toIndex);
        return new PagedResult<>(pageItems, normalizedPage, normalizedSize, items.size(), toIndex < items.size());
    }

    private void markRead(Long userId, String conversationKey) {
        Long lastMessageId = messageMapper.findLastMessageId(conversationKey);
        if (lastMessageId == null) {
            return;
        }
        ConversationStateRecord state = conversationStateMapper.findByUserAndConversation(userId, conversationKey);
        if (state == null) {
            ConversationStateRecord insertState = new ConversationStateRecord();
            insertState.setUserId(userId);
            insertState.setConversationKey(conversationKey);
            insertState.setLastReadMessageId(lastMessageId);
            insertState.setUpdatedAt(Instant.now());
            conversationStateMapper.insert(insertState);
            return;
        }
        conversationStateMapper.update(state.getId(), lastMessageId, Instant.now());
    }

    private Set<String> usernamesOfGroup(Long groupId) {
        Set<String> usernames = new LinkedHashSet<>();
        for (Long memberId : groupMemberMapper.listMemberIds(groupId)) {
            UserRecord user = userMapper.findById(memberId);
            if (user != null) {
                usernames.add(user.getUsername());
            }
        }
        return usernames;
    }

    private Set<String> allKnownUsernames() {
        return new LinkedHashSet<>(userMapper.listAllUsernames());
    }

    private StoredFile storeFile(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            throw new ChatException("请选择文件");
        }
        String originalName = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), prefix));
        String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String storedName = buildStoredFileName(prefix, safeName, file);
        Path target = resolveUploadDirectory(prefix).resolve(storedName);
        if (Files.exists(target)) {
            return new StoredFile(originalName, buildUploadUrl(prefix, storedName));
        }
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ChatException("文件保存失败");
        }
        return new StoredFile(originalName, buildUploadUrl(prefix, storedName));
    }

    private String buildStoredFileName(String prefix, String safeName, MultipartFile file) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(file.getBytes());
            return prefix + "-" + HexFormat.of().formatHex(digest) + "-" + safeName;
        } catch (NoSuchAlgorithmException | IOException ex) {
            throw new IllegalStateException("Unable to hash uploaded file", ex);
        }
    }

    private Path resolveUploadDirectory(String prefix) {
        if ("avatar".equals(prefix)) {
            return avatarUploadDir;
        }
        return chatUploadDir;
    }

    private String buildUploadUrl(String prefix, String storedName) {
        return "/uploads/" + prefix + "/" + storedName;
    }

    private String normalizeUsername(String username) {
        String value = Objects.requireNonNullElse(username, "").trim().toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z0-9_]{3,20}")) {
            throw new ChatException("用户名需为 3-20 位小写字母、数字或下划线");
        }
        return value;
    }

    private String normalizeDisplayName(String displayName, String fallback) {
        String value = Objects.requireNonNullElse(displayName, "").trim();
        if (value.isBlank()) {
            return fallback;
        }
        if (value.length() > 20) {
            throw new ChatException("昵称不能超过 20 个字符");
        }
        return value;
    }

    private String normalizeFriendRemark(String friendRemark) {
        String value = Objects.requireNonNullElse(friendRemark, "").trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() > 200) {
            throw new ChatException("好友备注不能超过 200 个字符");
        }
        return value;
    }

    private void validatePassword(String password) {
        String value = Objects.requireNonNullElse(password, "").trim();
        if (value.length() < 4 || value.length() > 32) {
            throw new ChatException("密码长度需为 4-32 位");
        }
    }

    private String directConversationKey(Long leftUserId, Long rightUserId) {
        long first = Math.min(leftUserId, rightUserId);
        long second = Math.max(leftUserId, rightUserId);
        return "direct:" + first + ":" + second;
    }

    private String groupConversationKey(Long groupId) {
        return "group:" + groupId;
    }

    private UserProfile toProfile(UserRecord user) {
        return new UserProfile(user.getId(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl());
    }

    private record StoredFile(String originalName, String url) {
    }
}
