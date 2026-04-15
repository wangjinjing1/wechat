const state = {
    me: null,
    users: [],
    friends: [],
    incomingFriendRequests: [],
    groups: [],
    lobbyUnread: 0,
    currentConversation: { type: "lobby", id: "lobby" },
    socket: null,
    suppressRefresh: false,
    messages: [],
    hasMoreMessages: false,
    nextBeforeId: null,
    loadingOlderMessages: false,
    skipNextRealtimeMessageEvents: 0,
    sidebar: {
        conversations: { items: [], page: 0, size: 8, keyword: "", hasMore: false, total: 0 },
        users: { items: [], page: 0, size: 8, keyword: "", hasMore: false, total: 0 },
        friendRequests: { items: [], page: 0, size: 8, keyword: "", hasMore: false, total: 0 },
        groupMembers: { items: [], page: 0, size: 12, keyword: "", hasMore: false, total: 0 }
    }
};

const authPanel = document.getElementById("auth-panel");
const chatApp = document.getElementById("chat-app");
const authMessage = document.getElementById("auth-message");
const chatMessage = document.getElementById("chat-message");
const userListEl = document.getElementById("user-list");
const friendRequestListEl = document.getElementById("friend-request-list");
const conversationListEl = document.getElementById("conversation-list");
const conversationSearchEl = document.getElementById("conversation-search");
const userSearchEl = document.getElementById("user-search");
const friendRequestSearchEl = document.getElementById("friend-request-search");
const conversationLoadMoreEl = document.getElementById("conversation-load-more");
const userLoadMoreEl = document.getElementById("user-load-more");
const friendRequestLoadMoreEl = document.getElementById("friend-request-load-more");
const messagesEl = document.getElementById("messages");
const meNameEl = document.getElementById("me-name");
const meUsernameEl = document.getElementById("me-username");
const meAvatarEl = document.getElementById("me-avatar");
const passwordFormEl = document.getElementById("password-form");
const conversationAvatarEl = document.getElementById("conversation-avatar");
const titleEl = document.getElementById("conversation-title");
const subtitleEl = document.getElementById("conversation-subtitle");
const friendRemarkBtnEl = document.getElementById("friend-remark-btn");
const removeFriendBtnEl = document.getElementById("remove-friend-btn");
const clearChatBtnEl = document.getElementById("clear-chat-btn");
const viewMembersBtnEl = document.getElementById("view-members-btn");
const groupPanelEl = document.getElementById("group-panel");
const openGroupModalBtnEl = document.getElementById("open-group-modal-btn");
const groupModalEl = document.getElementById("group-modal");
const groupModalBackdropEl = document.getElementById("group-modal-backdrop");
const groupModalCloseEl = document.getElementById("group-modal-close");
const memberViewerEl = document.getElementById("member-viewer");
const memberViewerBackdropEl = document.getElementById("member-viewer-backdrop");
const memberViewerCloseEl = document.getElementById("member-viewer-close");
const memberViewerTitleEl = document.getElementById("member-viewer-title");
const memberViewerCountEl = document.getElementById("member-viewer-count");
const memberViewerListEl = document.getElementById("member-viewer-list");
const memberViewerFormEl = document.getElementById("member-viewer-form");
const groupMemberSearchEl = document.getElementById("group-member-search");
const memberLoadMoreEl = document.getElementById("member-load-more");
const wsStatusEl = document.getElementById("ws-status");
const messageInput = document.getElementById("message-input");
const fileInput = document.getElementById("file-input");
const imageViewerEl = document.getElementById("image-viewer");
const imageViewerImageEl = document.getElementById("image-viewer-image");
const imageViewerCaptionEl = document.getElementById("image-viewer-caption");
const imageViewerCloseEl = document.getElementById("image-viewer-close");
const imageViewerBackdropEl = document.getElementById("image-viewer-backdrop");
const MAX_UPLOAD_SIZE = 20 * 1024 * 1024;

document.getElementById("login-tab").addEventListener("click", () => switchTab("login"));
document.getElementById("register-tab").addEventListener("click", () => switchTab("register"));
document.getElementById("login-form").addEventListener("submit", onLogin);
document.getElementById("register-form").addEventListener("submit", onRegister);
document.getElementById("logout-btn").addEventListener("click", onLogout);
document.getElementById("refresh-btn").addEventListener("click", async (event) => {
    event.preventDefault();
    event.stopPropagation();
    await refreshBootstrap("full");
    await loadConversationMessages({ reset: true, markSeen: false });
});
conversationSearchEl.addEventListener("input", () => onFilterChange("conversations", conversationSearchEl.value));
userSearchEl.addEventListener("input", () => onFilterChange("users", userSearchEl.value));
friendRequestSearchEl.addEventListener("input", () => onFilterChange("friendRequests", friendRequestSearchEl.value));
groupMemberSearchEl.addEventListener("input", () => onFilterChange("groupMembers", groupMemberSearchEl.value));
conversationLoadMoreEl.addEventListener("click", () => loadSidebarConversations(false));
userLoadMoreEl.addEventListener("click", () => loadSidebarUsers(false));
friendRequestLoadMoreEl.addEventListener("click", () => loadSidebarFriendRequests(false));
memberLoadMoreEl.addEventListener("click", () => loadSidebarGroupMembers(false));
document.getElementById("group-form").addEventListener("submit", onCreateGroup);
memberViewerFormEl.addEventListener("submit", onAddGroupMember);
openGroupModalBtnEl.addEventListener("click", openGroupModal);
groupModalCloseEl.addEventListener("click", closeGroupModal);
groupModalBackdropEl.addEventListener("click", closeGroupModal);
document.getElementById("avatar-input").addEventListener("change", onUploadAvatar);
passwordFormEl.addEventListener("submit", onChangePassword);
document.getElementById("message-form").addEventListener("submit", onSendMessage);
document.getElementById("message-input").addEventListener("keydown", onMessageKeydown);
document.getElementById("file-input").addEventListener("change", onSendFile);
document.querySelectorAll(".emoji-btn").forEach((button) => button.addEventListener("click", () => sendEmoji(button.dataset.emoji)));
messagesEl.addEventListener("scroll", onMessagesScroll);
friendRemarkBtnEl.addEventListener("click", onUpdateFriendRemark);
removeFriendBtnEl.addEventListener("click", onRemoveFriend);
clearChatBtnEl.addEventListener("click", onClearCurrentChat);
viewMembersBtnEl.addEventListener("click", openMemberViewer);
imageViewerCloseEl.addEventListener("click", closeImageViewer);
imageViewerBackdropEl.addEventListener("click", closeImageViewer);
memberViewerCloseEl.addEventListener("click", closeMemberViewer);
memberViewerBackdropEl.addEventListener("click", closeMemberViewer);
document.addEventListener("keydown", onGlobalKeydown);

init();

async function init() {
    try {
        await refreshBootstrap("full");
        showChat();
        connectSocket();
        await loadConversationMessages({ reset: true, markSeen: true });
    } catch {
        showAuth();
    }
}

function switchTab(type) {
    document.getElementById("login-tab").classList.toggle("active", type === "login");
    document.getElementById("register-tab").classList.toggle("active", type === "register");
    document.getElementById("login-form").classList.toggle("hidden", type !== "login");
    document.getElementById("register-form").classList.toggle("hidden", type !== "register");
    authMessage.textContent = "";
}

async function onLogin(event) {
    event.preventDefault();
    const form = new FormData(event.target);
    await submitAuth("/api/auth/login", {
        username: form.get("username"),
        password: form.get("password")
    });
}

async function onRegister(event) {
    event.preventDefault();
    const form = new FormData(event.target);
    await submitAuth("/api/auth/register", {
        username: form.get("username"),
        displayName: form.get("displayName"),
        password: form.get("password")
    });
}

async function submitAuth(url, payload) {
    try {
        await api(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        await refreshBootstrap("full");
        showChat();
        connectSocket();
        await loadConversationMessages({ reset: true, markSeen: true });
    } catch (error) {
        authMessage.textContent = error.message;
    }
}

async function onLogout() {
    await api("/api/auth/logout", { method: "POST" }).catch(() => {});
    disconnectSocket();
    showAuth();
}

function openGroupModal() {
    groupModalEl.classList.remove("hidden");
    groupModalEl.setAttribute("aria-hidden", "false");
    document.body.style.overflow = "hidden";
}

function closeGroupModal() {
    groupModalEl.classList.add("hidden");
    groupModalEl.setAttribute("aria-hidden", "true");
    document.body.style.overflow = imageViewerEl.classList.contains("hidden") && memberViewerEl.classList.contains("hidden") ? "" : "hidden";
}

async function onChangePassword(event) {
    event.preventDefault();
    const form = new FormData(event.target);
    try {
        await api("/api/auth/password", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                currentPassword: form.get("currentPassword"),
                newPassword: form.get("newPassword"),
                confirmPassword: form.get("confirmPassword")
            })
        });
        event.target.reset();
        setChatMessage("密码已修改");
    } catch (error) {
        setChatMessage(error.message);
    }
}

async function refreshBootstrap(mode = "full") {
    const data = await api("/api/bootstrap");
    state.me = data.me;
    state.users = data.users;
    state.friends = data.friends;
    state.incomingFriendRequests = data.incomingFriendRequests || [];
    state.groups = data.groups;
    state.lobbyUnread = data.lobbyUnread;
    ensureConversationStillExists();

    if (mode === "full") {
        renderCurrentProfile();
        await Promise.all([
            loadSidebarConversations(true),
            loadSidebarUsers(true),
            loadSidebarFriendRequests(true)
        ]);
        renderCurrentConversationMeta();
        return;
    }

    if (mode === "conversation-list") {
        await loadSidebarConversations(true);
    }
}

async function loadSidebarConversations(reset) {
    await loadSidebarSection("conversations", "/api/sidebar/conversations", reset, renderConversationList, conversationLoadMoreEl);
}

async function loadSidebarUsers(reset) {
    await loadSidebarSection("users", "/api/sidebar/users", reset, renderUsers, userLoadMoreEl);
}

async function loadSidebarFriendRequests(reset) {
    await loadSidebarSection("friendRequests", "/api/sidebar/friend-requests", reset, renderFriendRequests, friendRequestLoadMoreEl);
}

async function loadSidebarGroupMembers(reset) {
    const group = getCurrentGroup();
    if (!group) {
        state.sidebar.groupMembers = { ...state.sidebar.groupMembers, items: [], page: 0, hasMore: false, total: 0 };
        renderMemberViewer();
        return;
    }
    await loadSidebarSection("groupMembers", `/api/groups/${group.id}/members`, reset, renderMemberViewer, memberLoadMoreEl);
}

async function loadSidebarSection(key, baseUrl, reset, renderFn, loadMoreButton) {
    const sidebarState = state.sidebar[key];
    const nextPage = reset ? 0 : sidebarState.page + 1;
    const query = new URLSearchParams({
        page: String(nextPage),
        size: String(sidebarState.size)
    });
    if (sidebarState.keyword) {
        query.set("keyword", sidebarState.keyword);
    }
    const pageData = await api(`${baseUrl}?${query.toString()}`);
    state.sidebar[key] = {
        ...sidebarState,
        items: reset ? pageData.items : [...sidebarState.items, ...pageData.items],
        page: pageData.page,
        size: pageData.size,
        total: pageData.total,
        hasMore: pageData.hasMore
    };
    loadMoreButton.classList.toggle("hidden", !pageData.hasMore);
    renderFn();
}

function ensureConversationStillExists() {
    if (state.currentConversation.type === "direct" &&
        !state.friends.some((friend) => friend.username === state.currentConversation.id)) {
        state.currentConversation = { type: "lobby", id: "lobby" };
    }
    if (state.currentConversation.type === "group" &&
        !state.groups.some((group) => String(group.id) === String(state.currentConversation.id))) {
        state.currentConversation = { type: "lobby", id: "lobby" };
    }
}

function renderCurrentProfile() {
    meNameEl.textContent = state.me.displayName;
    meUsernameEl.textContent = `@${state.me.username}`;
    renderAvatar(meAvatarEl, state.me.avatarUrl, state.me.displayName);
}

function onFilterChange(key, value) {
    state.sidebar[key].keyword = String(value || "").trim();
    if (key === "conversations") {
        loadSidebarConversations(true);
        return;
    }
    if (key === "users") {
        loadSidebarUsers(true);
        return;
    }
    if (key === "friendRequests") {
        loadSidebarFriendRequests(true);
        return;
    }
    if (key === "groupMembers") {
        loadSidebarGroupMembers(true);
    }
}

function getConversationItems() {
    return state.sidebar.conversations.items;
}

function renderConversationList(fullRender) {
    const items = getConversationItems();
    if (!items.length) {
        conversationListEl.innerHTML = '<p class="muted">没有匹配的会话</p>';
        return;
    }
    if (!fullRender) {
        if (patchConversationList(items)) {
            return;
        }
    }

    conversationListEl.innerHTML = items.map(renderConversationButton).join("");
    bindConversationListEvents();
}

function patchConversationList(items) {
    const buttons = Array.from(conversationListEl.querySelectorAll(".conversation-item"));
    if (buttons.length !== items.length) {
        return false;
    }

    for (const item of items) {
        const button = conversationListEl.querySelector(`.conversation-item[data-type="${item.type}"][data-id="${cssEscape(item.id)}"]`);
        if (!button) {
            return false;
        }
        button.classList.toggle("active", isCurrentConversation(item));
        syncBadge(button.querySelector(".conversation-meta"), item.unreadCount);
    }
    return true;
}

function bindConversationListEvents() {
    conversationListEl.querySelectorAll(".conversation-item").forEach((button) => {
        button.addEventListener("click", async () => {
            state.currentConversation = { type: button.dataset.type, id: button.dataset.id };
            renderConversationList(false);
            renderCurrentConversationMeta();
            await loadConversationMessages({ reset: true, markSeen: true });
        });
    });
}

function renderConversationButton(item) {
    return `
        <button class="conversation-item ${isCurrentConversation(item) ? "active" : ""}" data-type="${item.type}" data-id="${escapeHtml(item.id)}">
            <div class="conversation-meta">
                <div class="header-main">
                    <div class="avatar">${item.avatarUrl ? `<img src="${escapeHtml(item.avatarUrl)}" alt="">` : avatarText(item.title)}</div>
                    <div>
                        <strong>${escapeHtml(item.title)}</strong>
                        <div class="muted">${escapeHtml(item.subtitle)}</div>
                    </div>
                </div>
                ${item.unreadCount > 0 ? `<span class="badge">${item.unreadCount}</span>` : ""}
            </div>
        </button>
    `;
}

function renderUsers() {
    const items = state.sidebar.users.items;
    if (!items.length) {
        userListEl.innerHTML = '<p class="muted">没有匹配的用户</p>';
        return;
    }

    userListEl.innerHTML = items.map((user) => `
        <div class="user-card">
            <div class="header-main">
                <div class="avatar">${user.avatarUrl ? `<img src="${escapeHtml(user.avatarUrl)}" alt="">` : avatarText(user.displayName)}</div>
                <div>
                    <strong>${escapeHtml(user.displayName)}</strong>
                    <div class="muted">@${escapeHtml(user.username)}</div>
                </div>
            </div>
            ${renderUserAction(user)}
        </div>
    `).join("");

    userListEl.querySelectorAll("[data-add]").forEach((button) => {
        button.addEventListener("click", () => addFriend(button.dataset.add));
    });
}

function renderUserAction(user) {
    if (user.friend) {
        return '<span class="badge">好友</span>';
    }
    if (user.outgoingRequest) {
        return '<span class="badge">待同意</span>';
    }
    if (user.incomingRequest) {
        return '<span class="badge">待处理</span>';
    }
    return `<button class="mini-btn" data-add="${escapeHtml(user.username)}">添加</button>`;
}

function renderFriendRequests() {
    const items = state.sidebar.friendRequests.items;
    if (!items.length) {
        friendRequestListEl.innerHTML = '<p class="muted">暂无待处理申请</p>';
        return;
    }

    friendRequestListEl.innerHTML = items.map((request) => `
        <div class="user-card">
            <div class="header-main">
                <div class="avatar">${request.requesterAvatarUrl ? `<img src="${escapeHtml(request.requesterAvatarUrl)}" alt="">` : avatarText(request.requesterDisplayName)}</div>
                <div>
                    <strong>${escapeHtml(request.requesterDisplayName)}</strong>
                    <div class="muted">@${escapeHtml(request.requesterUsername)}</div>
                </div>
            </div>
            <div class="composer-actions">
                <button class="mini-btn" data-accept-request="${request.id}">同意</button>
                <button class="mini-btn danger-btn" data-reject-request="${request.id}">拒绝</button>
            </div>
        </div>
    `).join("");

    friendRequestListEl.querySelectorAll("[data-accept-request]").forEach((button) => {
        button.addEventListener("click", () => acceptFriendRequest(button.dataset.acceptRequest));
    });
    friendRequestListEl.querySelectorAll("[data-reject-request]").forEach((button) => {
        button.addEventListener("click", () => rejectFriendRequest(button.dataset.rejectRequest));
    });
}

function renderCurrentConversationMeta() {
    if (state.currentConversation.type === "lobby") {
        titleEl.textContent = "聊天大厅";
        subtitleEl.textContent = "所有用户都可见";
        renderAvatar(conversationAvatarEl, null, "聊天大厅");
        friendRemarkBtnEl.classList.add("hidden");
        removeFriendBtnEl.classList.add("hidden");
        viewMembersBtnEl.classList.add("hidden");
        groupPanelEl.classList.add("hidden");
        return;
    }

    if (state.currentConversation.type === "direct") {
        const friend = state.friends.find((item) => item.username === state.currentConversation.id);
        titleEl.textContent = friend?.friendRemark || friend?.displayName || "Direct chat";
        subtitleEl.textContent = friend
            ? `${friend.displayName}${friend.friendRemark ? ` · @${friend.username}` : ` @${friend.username}`}`
            : "Direct chat";
        renderAvatar(conversationAvatarEl, friend?.avatarUrl, friend?.friendRemark || friend?.displayName || "Direct");
        friendRemarkBtnEl.classList.remove("hidden");
        removeFriendBtnEl.classList.remove("hidden");
        viewMembersBtnEl.classList.add("hidden");
        groupPanelEl.classList.add("hidden");
        return;
    }

    const group = getCurrentGroup();
    titleEl.textContent = group?.name || "Group";
    subtitleEl.textContent = group ? `${group.members.length} members` : "Group chat";
    renderAvatar(conversationAvatarEl, group?.avatarUrl, group?.name || "Group");
    friendRemarkBtnEl.classList.add("hidden");
    removeFriendBtnEl.classList.add("hidden");
    viewMembersBtnEl.classList.remove("hidden");
    renderGroupPanel(group);
}

function renderGroupPanel(group) {
    groupPanelEl.classList.add("hidden");
    if (!group) {
        closeMemberViewer();
    }
}

function openMemberViewer() {
    const group = getCurrentGroup();
    if (!group) {
        return;
    }
    state.filters.groupMembers = "";
    groupMemberSearchEl.value = "";
    renderMemberViewer();
    memberViewerEl.classList.remove("hidden");
    memberViewerEl.setAttribute("aria-hidden", "false");
    document.body.style.overflow = "hidden";
}

function closeMemberViewer() {
    memberViewerEl.classList.add("hidden");
    memberViewerEl.setAttribute("aria-hidden", "true");
    document.body.style.overflow = imageViewerEl.classList.contains("hidden") ? "" : "hidden";
}

function renderMemberViewer() {
    const group = getCurrentGroup();
    if (!group) {
        memberViewerListEl.innerHTML = "";
        return;
    }

    const filteredMembers = group.members.filter((member) =>
        matchesKeyword([member.displayName, member.username, member.role], state.filters.groupMembers)
    );
    memberViewerTitleEl.textContent = `${group.name} 成员`;
    memberViewerCountEl.textContent = `${filteredMembers.length} / ${group.members.length}`;
    memberViewerFormEl.classList.toggle("hidden", group.myRole !== "OWNER");

    if (!filteredMembers.length) {
        memberViewerListEl.innerHTML = '<p class="muted">没有匹配的群成员</p>';
        return;
    }

    memberViewerListEl.innerHTML = filteredMembers.map((member) => `
        <div class="member-card">
            <div class="header-main">
                <div class="avatar">${member.avatarUrl ? `<img src="${escapeHtml(member.avatarUrl)}" alt="">` : avatarText(member.displayName)}</div>
                <div>
                    <strong>${escapeHtml(member.displayName)}</strong>
                    <div class="muted">@${escapeHtml(member.username)} ${member.role === "OWNER" ? "群主" : "成员"}</div>
                </div>
            </div>
            ${group.myRole === "OWNER" && member.role !== "OWNER"
                ? `<button class="mini-btn danger-btn" data-remove-member="${member.userId}">移除</button>`
                : ""}
        </div>
    `).join("");

    memberViewerListEl.querySelectorAll("[data-remove-member]").forEach((button) => {
        button.addEventListener("click", () => removeGroupMember(button.dataset.removeMember));
    });
}

async function addFriend(username) {
    try {
        await api(`/api/friends/${encodeURIComponent(username)}`, { method: "POST" });
        await refreshBootstrap("full");
        setChatMessage(`Friend request sent to @${username}`);
    } catch (error) {
        setChatMessage(error.message);
    }
}

async function acceptFriendRequest(requestId) {
    try {
        await api(`/api/friend-requests/${requestId}/accept`, { method: "POST" });
        await refreshBootstrap("full");
        setChatMessage("Friend request accepted");
    } catch (error) {
        setChatMessage(error.message);
    }
}

async function rejectFriendRequest(requestId) {
    try {
        await api(`/api/friend-requests/${requestId}/reject`, { method: "POST" });
        await refreshBootstrap("full");
        setChatMessage("Friend request rejected");
    } catch (error) {
        setChatMessage(error.message);
    }
}

async function onUpdateFriendRemark() {
    if (state.currentConversation.type !== "direct") {
        return;
    }
    const friend = state.friends.find((item) => item.username === state.currentConversation.id);
    if (!friend) {
        return;
    }
    const remark = window.prompt("输入好友备注，留空则清除", friend.friendRemark || "");
    if (remark === null) {
        return;
    }
    try {
        await api(`/api/friends/${encodeURIComponent(friend.username)}/remark`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ remark })
        });
        await refreshBootstrap("full");
        setChatMessage("好友备注已更新");
    } catch (error) {
        setChatMessage(error.message);
    }
}

async function onRemoveFriend() {
    if (state.currentConversation.type !== "direct") {
        return;
    }
    const friend = state.friends.find((item) => item.username === state.currentConversation.id);
    if (!friend || !window.confirm(`确认删除好友 ${friend.displayName} 吗？`)) {
        return;
    }
    try {
        await api(`/api/friends/${encodeURIComponent(friend.username)}/remove`, { method: "POST" });
        state.currentConversation = { type: "lobby", id: "lobby" };
        await refreshBootstrap("full");
        renderConversationList(false);
        renderCurrentConversationMeta();
        await loadConversationMessages({ reset: true, markSeen: true });
        setChatMessage("好友已删除");
    } catch (error) {
        setChatMessage(error.message);
    }
}

async function onClearCurrentChat() {
    const label = getCurrentConversationLabel();
    if (!window.confirm(`确认只在当前浏览器清空“${label}”的聊天记录吗？数据库中的消息不会删除。`)) {
        return;
    }
    if (state.messages.length) {
        saveConversationClearMarker(state.messages[state.messages.length - 1].id);
    } else {
        saveConversationClearMarker(Number.MAX_SAFE_INTEGER);
    }
    renderMessages(state.messages);
    setChatMessage("当前浏览器中的聊天记录已清空");
}

async function onCreateGroup(event) {
    event.preventDefault();
    const form = new FormData(event.target);
    const members = String(form.get("members") || "")
        .split(",")
        .map((item) => item.trim())
        .filter(Boolean);

    try {
        const group = await api("/api/groups", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name: form.get("name"), memberUsernames: members })
        });
        event.target.reset();
        closeGroupModal();
        await refreshBootstrap("full");
        state.currentConversation = { type: "group", id: String(group.id) };
        renderCurrentConversationMeta();
        await loadConversationMessages({ reset: true, markSeen: true });
        setChatMessage(`已创建群聊 ${group.name}`);
    } catch (error) {
        setChatMessage(error.message);
    }
}

async function onAddGroupMember(event) {
    event.preventDefault();
    const group = getCurrentGroup();
    if (!group) {
        return;
    }
    const form = new FormData(event.target);

    try {
        await api(`/api/groups/${group.id}/members`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username: form.get("username") })
        });
        event.target.reset();
        await refreshBootstrap("full");
        renderMemberViewer();
        setChatMessage("Member added");
    } catch (error) {
        setChatMessage(error.message);
    }
}

async function removeGroupMember(memberUserId) {
    const group = getCurrentGroup();
    if (!group) {
        return;
    }

    try {
        await api(`/api/groups/${group.id}/members/${memberUserId}/remove`, { method: "POST" });
        await refreshBootstrap("full");
        renderMemberViewer();
        setChatMessage("Member removed");
    } catch (error) {
        setChatMessage(error.message);
    }
}

async function onUploadAvatar() {
    const input = document.getElementById("avatar-input");
    if (!input.files.length) {
        return;
    }
    if (!validateUploadFile(input.files[0], input)) {
        return;
    }

    const form = new FormData();
    form.append("file", input.files[0]);
    try {
        await api("/api/me/avatar", { method: "POST", body: form });
        input.value = "";
        await refreshBootstrap("full");
        setChatMessage("Avatar updated");
    } catch (error) {
        setChatMessage(error.message);
    }
}

async function loadConversationMessages({ reset, markSeen }) {
    if (state.loadingOlderMessages) {
        return;
    }

    state.suppressRefresh = true;
    try {
        if (reset) {
            messagesEl.classList.add("messages-positioning");
        }
        const beforeId = reset ? null : state.nextBeforeId;
        if (!reset && (!state.hasMoreMessages || beforeId == null)) {
            return;
        }

        state.loadingOlderMessages = true;
        const page = await api(conversationUrl(beforeId));

        state.hasMoreMessages = page.hasMore;
        state.nextBeforeId = page.nextBeforeId;

        if (reset) {
            state.messages = page.items;
            renderMessages(page.items);
            if (markSeen) {
                await refreshBootstrap("conversation-list");
            }
            scheduleScrollMessagesToBottom(() => {
                messagesEl.classList.remove("messages-positioning");
            });
            return;
        }

        prependMessages(page.items);
    } finally {
        state.loadingOlderMessages = false;
        state.suppressRefresh = false;
        if (!reset) {
            messagesEl.classList.remove("messages-positioning");
        }
    }
}

function renderMessages(messages) {
    state.messages = messages;
    messagesEl.innerHTML = `<div class="message-list">${getVisibleMessages(messages).map(renderMessageCard).join("")}</div>`;
    bindMessagePreviewEvents(messagesEl);
}

function prependMessages(messages) {
    if (!messages.length) {
        return;
    }

    const listEl = messagesEl.querySelector(".message-list");
    if (!listEl) {
        renderMessages(messages);
        return;
    }

    const previousHeight = messagesEl.scrollHeight;
    state.messages = [...messages, ...state.messages];
    const visibleMessages = getVisibleMessages(messages);
    if (!visibleMessages.length) {
        return;
    }
    listEl.insertAdjacentHTML("afterbegin", visibleMessages.map(renderMessageCard).join(""));
    bindMessagePreviewEvents(listEl);
    const heightDiff = messagesEl.scrollHeight - previousHeight;
    messagesEl.scrollTop += heightDiff;
}

function appendMessage(message) {
    if (state.messages.some((item) => item.id === message.id)) {
        return;
    }

    const listEl = messagesEl.querySelector(".message-list");
    const stickToBottom = isNearBottom();
    state.messages = [...state.messages, message];
    if (!shouldShowMessage(message)) {
        return;
    }

    if (!listEl) {
        renderMessages(state.messages);
    } else {
        listEl.insertAdjacentHTML("beforeend", renderMessageCard(message));
        bindMessagePreviewEvents(listEl);
    }

    if (stickToBottom) {
        scheduleScrollMessagesToBottom();
    }
}

function renderMessageCard(message) {
    return `
        <article class="message-card" data-message-id="${message.id}">
            <div class="message-head">
                <div class="message-author">
                    <div class="avatar">${message.senderAvatarUrl ? `<img src="${escapeHtml(message.senderAvatarUrl)}" alt="">` : avatarText(message.senderName)}</div>
                    <div>
                        <strong>${escapeHtml(message.senderName)}</strong>
                        <div class="muted">${formatTime(message.createdAt)}</div>
                    </div>
                </div>
            </div>
            <div class="message-body">${renderMessageBody(message)}</div>
        </article>
    `;
}

function bindMessagePreviewEvents(container) {
    container.querySelectorAll("[data-preview-image]").forEach((button) => {
        if (button.dataset.boundPreview === "true") {
            return;
        }
        button.dataset.boundPreview = "true";
        button.addEventListener("click", () => openImageViewer(button.dataset.previewImage, button.dataset.previewName || ""));
    });
}

async function onSendMessage(event) {
    event.preventDefault();
    const content = messageInput.value.trim();
    if (!content) {
        return;
    }

    try {
        state.skipNextRealtimeMessageEvents += 1;
        const message = await api(conversationUrl(), {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ type: "TEXT", content })
        });
        messageInput.value = "";
        appendMessage(message);
    } catch (error) {
        setChatMessage(error.message);
    }
}

function onMessageKeydown(event) {
    if (event.key === "Enter" && !event.shiftKey) {
        event.preventDefault();
        document.getElementById("message-form").requestSubmit();
    }
}

async function sendEmoji(emoji) {
    try {
        state.skipNextRealtimeMessageEvents += 1;
        const message = await api(conversationUrl(), {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ type: "EMOJI", content: emoji })
        });
        appendMessage(message);
    } catch (error) {
        setChatMessage(error.message);
    }
}

async function onSendFile() {
    if (!fileInput.files.length) {
        return;
    }
    if (!validateUploadFile(fileInput.files[0], fileInput)) {
        return;
    }

    const form = new FormData();
    form.append("file", fileInput.files[0]);
    try {
        state.skipNextRealtimeMessageEvents += 1;
        const message = await api(fileUrl(), { method: "POST", body: form });
        fileInput.value = "";
        appendMessage(message);
    } catch (error) {
        setChatMessage(error.message);
    }
}

function onMessagesScroll() {
    if (messagesEl.scrollTop < 80 && state.hasMoreMessages && !state.loadingOlderMessages) {
        loadConversationMessages({ reset: false, markSeen: false });
    }
}

function connectSocket() {
    disconnectSocket();
    const scheme = location.protocol === "https:" ? "wss" : "ws";
    state.socket = new WebSocket(`${scheme}://${location.host}/ws/chat`);
    state.socket.onopen = () => updateWsStatus(true);
    state.socket.onclose = () => updateWsStatus(false);
    state.socket.onerror = () => updateWsStatus(false);
    state.socket.onmessage = async (event) => {
        const payload = JSON.parse(event.data);
        if (payload.type === "message") {
            await handleRealtimeMessage(payload);
            return;
        }
        if (payload.type === "refresh" && !state.suppressRefresh) {
            await refreshBootstrap("full");
        }
    };
}

async function handleRealtimeMessage(payload) {
    if (state.skipNextRealtimeMessageEvents > 0 && payload.senderUsername === state.me?.username) {
        state.skipNextRealtimeMessageEvents -= 1;
        return;
    }

    if (isCurrentConversationEvent(payload)) {
        appendMessage(payload.message);
        if (payload.senderUsername !== state.me?.username) {
            await markCurrentConversationRead();
        }
        return;
    }

    await refreshBootstrap("conversation-list");
}

function isCurrentConversationEvent(payload) {
    if (payload.scope === "LOBBY") {
        return state.currentConversation.type === "lobby";
    }
    if (payload.scope === "GROUP") {
        return state.currentConversation.type === "group"
            && String(payload.groupId) === String(state.currentConversation.id);
    }
    if (payload.scope === "DIRECT") {
        return state.currentConversation.type === "direct"
            && [payload.senderUsername, payload.targetUsername].includes(state.currentConversation.id);
    }
    return false;
}

function disconnectSocket() {
    if (state.socket) {
        state.socket.close();
        state.socket = null;
    }
    updateWsStatus(false);
}

function updateWsStatus(online) {
    wsStatusEl.textContent = online ? "Online" : "Offline";
    wsStatusEl.classList.toggle("online", online);
    wsStatusEl.classList.toggle("offline", !online);
}

function showChat() {
    authPanel.classList.add("hidden");
    chatApp.classList.remove("hidden");
}

function showAuth() {
    chatApp.classList.add("hidden");
    authPanel.classList.remove("hidden");
}

function getCurrentGroup() {
    return state.groups.find((group) => String(group.id) === String(state.currentConversation.id));
}

function conversationUrl(beforeId = null) {
    let baseUrl;
    if (state.currentConversation.type === "lobby") {
        baseUrl = "/api/conversations/lobby/messages";
    } else if (state.currentConversation.type === "direct") {
        baseUrl = `/api/conversations/direct/${encodeURIComponent(state.currentConversation.id)}/messages`;
    } else {
        baseUrl = `/api/conversations/group/${encodeURIComponent(state.currentConversation.id)}/messages`;
    }

    if (beforeId == null) {
        return baseUrl;
    }
    return `${baseUrl}?beforeId=${encodeURIComponent(beforeId)}`;
}

function fileUrl() {
    if (state.currentConversation.type === "lobby") {
        return "/api/conversations/lobby/files";
    }
    if (state.currentConversation.type === "direct") {
        return `/api/conversations/direct/${encodeURIComponent(state.currentConversation.id)}/files`;
    }
    return `/api/conversations/group/${encodeURIComponent(state.currentConversation.id)}/files`;
}

function renderAvatar(container, url, text) {
    container.innerHTML = url ? `<img src="${escapeHtml(url)}" alt="">` : avatarText(text);
}

function renderMessageBody(message) {
    if (message.contentType === "FILE") {
        if (isImageFile(message.fileName, message.fileUrl)) {
            const imageUrl = escapeHtml(message.fileUrl);
            const imageName = escapeHtml(message.fileName || "Image");
            return `
                <button class="message-image" type="button" data-preview-image="${imageUrl}" data-preview-name="${imageName}">
                    <img src="${imageUrl}" alt="${imageName}" loading="lazy">
                    <span class="message-image-name">${imageName}</span>
                </button>
            `;
        }
        return `<a class="message-file" href="${escapeHtml(message.fileUrl)}" target="_blank">${escapeHtml(message.fileName)}</a>`;
    }
    return escapeHtml(message.content).replace(/\n/g, "<br>");
}

function isImageFile(fileName, fileUrl) {
    const value = String(fileName || fileUrl || "").toLowerCase();
    return [".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp", ".svg"].some((ext) => value.endsWith(ext));
}

function openImageViewer(url, name) {
    imageViewerImageEl.src = url;
    imageViewerImageEl.alt = name || "Image preview";
    imageViewerCaptionEl.textContent = name || "";
    imageViewerEl.classList.remove("hidden");
    imageViewerEl.setAttribute("aria-hidden", "false");
    document.body.style.overflow = "hidden";
}

function closeImageViewer() {
    imageViewerEl.classList.add("hidden");
    imageViewerEl.setAttribute("aria-hidden", "true");
    imageViewerImageEl.removeAttribute("src");
    imageViewerCaptionEl.textContent = "";
    document.body.style.overflow = "";
}

function onGlobalKeydown(event) {
    if (event.key === "Escape" && !imageViewerEl.classList.contains("hidden")) {
        closeImageViewer();
        return;
    }
    if (event.key === "Escape" && !memberViewerEl.classList.contains("hidden")) {
        closeMemberViewer();
    }
}

function avatarText(text) {
    return escapeHtml(String(text || "?").slice(0, 1).toUpperCase());
}

function isCurrentConversation(item) {
    return state.currentConversation.type === item.type
        && String(state.currentConversation.id || "lobby") === String(item.id);
}

function syncBadge(container, unreadCount) {
    const badge = container.querySelector(".badge");
    if (unreadCount > 0) {
        if (badge) {
            badge.textContent = unreadCount;
            return;
        }
        container.insertAdjacentHTML("beforeend", `<span class="badge">${unreadCount}</span>`);
        return;
    }
    if (badge) {
        badge.remove();
    }
}

function scrollMessagesToBottom() {
    messagesEl.scrollTop = messagesEl.scrollHeight;
}

function scheduleScrollMessagesToBottom(callback) {
    requestAnimationFrame(() => {
        scrollMessagesToBottom();
        requestAnimationFrame(() => {
            scrollMessagesToBottom();
            callback?.();
        });
    });
}

function isNearBottom() {
    return messagesEl.scrollHeight - messagesEl.scrollTop - messagesEl.clientHeight < 80;
}

function getCurrentConversationStorageKey() {
    if (state.currentConversation.type === "lobby") {
        return "lobby";
    }
    if (state.currentConversation.type === "direct") {
        return `direct:${state.currentConversation.id}`;
    }
    return `group:${state.currentConversation.id}`;
}

function getConversationClearMarkers() {
    try {
        return JSON.parse(localStorage.getItem("wechat-cleared-conversations") || "{}");
    } catch {
        return {};
    }
}

function saveConversationClearMarker(messageId) {
    const markers = getConversationClearMarkers();
    markers[getCurrentConversationStorageKey()] = Number(messageId) || 0;
    localStorage.setItem("wechat-cleared-conversations", JSON.stringify(markers));
}

function getCurrentConversationClearMarker() {
    const markers = getConversationClearMarkers();
    return Number(markers[getCurrentConversationStorageKey()] || 0);
}

function shouldShowMessage(message) {
    return Number(message.id) > getCurrentConversationClearMarker();
}

function getVisibleMessages(messages) {
    return messages.filter(shouldShowMessage);
}

function getCurrentConversationLabel() {
    if (state.currentConversation.type === "lobby") {
        return "聊天大厅";
    }
    if (state.currentConversation.type === "direct") {
        const friend = state.friends.find((item) => item.username === state.currentConversation.id);
        return friend?.friendRemark || friend?.displayName || state.currentConversation.id;
    }
    const group = getCurrentGroup();
    return group?.name || "群聊";
}

async function markCurrentConversationRead() {
    try {
        await api(conversationUrl());
    } catch {
    }
}

async function api(url, options = {}) {
    const response = await fetch(url, { credentials: "same-origin", ...options });
    if (response.status === 204) {
        return null;
    }
    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) {
        throw new Error(data?.error || `Request failed: ${response.status}`);
    }
    return data;
}

function setChatMessage(message) {
    chatMessage.textContent = message || "";
}

function validateUploadFile(file, input) {
    if (!file) {
        return false;
    }
    if (file.size <= MAX_UPLOAD_SIZE) {
        return true;
    }
    input.value = "";
    window.alert("文件不能超过 20MB");
    return false;
}

function formatTime(value) {
    return new Date(value).toLocaleString("zh-CN", {
        hour12: false,
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
    });
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

function cssEscape(value) {
    return String(value).replaceAll("\\", "\\\\").replaceAll('"', '\\"');
}
